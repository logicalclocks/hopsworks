/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.api.featurestore.FeaturestoreService;
import io.hops.hopsworks.api.featurestore.featuregroup.json.FeaturegroupJsonDTO;
import io.hops.hopsworks.api.featurestore.util.FeaturestoreUtil;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.RowValueQueryResult;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Stateless RESTful service for the featuregroups in a featurestore on Hopsworks.
 * Base URL: project/projectId/featurestores/featurestoreId/featuregroups/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Featuregroup service", description = "A service that manages a feature store's feature groups")
public class FeaturegroupService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private FeaturestoreUtil featurestoreUtil;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;
  private Featurestore featurestore;
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreService.class.getName());

  /**
   * Set the project of the featurestore (provided by parent resource)
   *
   * @param project the project where the featurestore resides
   */
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Sets the featurestore of the featuregroups (provided by parent resource)
   *
   * @param featurestoreId id of the featurestore
   * @throws FeaturestoreException
   */
  public void setFeaturestoreId(Integer featurestoreId) throws FeaturestoreException {
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    this.featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
  }

  /**
   * Endpoint for getting all featuregroups of a featurestore
   *
   * @return list of JSON featuregroups
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of feature groups for a featurestore",
      response = FeaturegroupDTO.class,
      responseContainer = "List")
  public Response getFeaturegroupsForFeaturestore() throws FeaturestoreException {
    List<FeaturegroupDTO> featuregroups = featuregroupController.
        getFeaturegroupsForFeaturestore(featurestore);
    GenericEntity<List<FeaturegroupDTO>> featuregroupsGeneric =
        new GenericEntity<List<FeaturegroupDTO>>(featuregroups) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupsGeneric).build();
  }

  /**
   * Endpoint for creating a new featuregroup in a featurestore
   *
   * @param featuregroupJsonDTO JSON payload for the new featuregroup
   * @return JSON information about the created featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create feature group in a featurestore",
      response = FeaturegroupDTO.class)
  public Response createFeaturegroup(@Context SecurityContext sc, FeaturegroupJsonDTO featuregroupJsonDTO)
      throws FeaturestoreException, HopsSecurityException {
    if (featuregroupJsonDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
            Settings.HOPS_FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Jobs job = null;
    if (featuregroupJsonDTO.getJobName() != null) {
      job = jobFacade.findByProjectAndName(project, featuregroupJsonDTO.getJobName());
    }
    try {
      featuregroupController.deleteFeaturegroupIfExists(featurestore, null, project, user,
          featuregroupJsonDTO.getName(), featuregroupJsonDTO.getVersion());
      FeaturegroupDTO featuregroupDTO = null;
      if(featuregroupJsonDTO.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP){
        featuregroupController.verifyCachedFeaturegroupUserInput(featuregroupJsonDTO.getName(),
            featuregroupJsonDTO.getDescription(), featuregroupJsonDTO.getFeatures());
        String featureStr = featurestoreUtil.makeCreateTableColumnsStr(featuregroupJsonDTO.getFeatures(),
            featuregroupJsonDTO.getDescription());
        featuregroupDTO = featuregroupController.createCachedFeaturegroup(project, user, featurestore,
            featuregroupJsonDTO.getName(), featureStr, job, featuregroupJsonDTO.getVersion(),
            featuregroupJsonDTO.getFeatureCorrelationMatrix(), featuregroupJsonDTO.getDescriptiveStatistics(),
            featuregroupJsonDTO.getFeaturesHistogram(), featuregroupJsonDTO.getClusterAnalysis());
      } else {
        featuregroupController.verifyOnDemandFeaturegroupUserInput(featuregroupJsonDTO.getName(),
            featuregroupJsonDTO.getDescription(), featuregroupJsonDTO.getJdbcConnectorId(),
            featuregroupJsonDTO.getSqlQuery(), featuregroupJsonDTO.getFeatures());
        featuregroupDTO = featuregroupController.createOnDemandFeaturegroup(project, user, featurestore,
            featuregroupJsonDTO.getName(), featuregroupJsonDTO.getFeatures(), job, featuregroupJsonDTO.getVersion(),
            featuregroupJsonDTO.getFeatureCorrelationMatrix(), featuregroupJsonDTO.getDescriptiveStatistics(),
            featuregroupJsonDTO.getFeaturesHistogram(), featuregroupJsonDTO.getClusterAnalysis(),
            featuregroupJsonDTO.getJdbcConnectorId(), featuregroupJsonDTO.getSqlQuery(),
            featuregroupJsonDTO.getDescription());
      }
      activityFacade.persistActivity(ActivityFacade.CREATED_FEATUREGROUP + featuregroupDTO.getName(),
          project, user, ActivityFlag.SERVICE);
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featuregroupGeneric).build();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId(), e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the featuregroup
   */
  @GET
  @Path("/{featuregroupId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get specific featuregroup from a specific featurestore",
      response = FeaturegroupDTO.class)
  public Response getFeatureGroupFromFeatureStore(@ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId")
          Integer featuregroupId) throws FeaturestoreException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    FeaturegroupDTO featuregroupDTO =
        featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
    GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
  }

  /**
   * Endpoint for deleting a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the deleted featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featuregroupId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete specific featuregroup from a specific featurestore",
      response = FeaturegroupDTO.class)
  public Response deleteFeatureGroupFromFeatureStore(
      @Context SecurityContext sc, @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //Verify that the user has the data-owner role or is the creator of the featuregroup
    FeaturegroupDTO featuregroupDTO = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featuregroupId);
    featurestoreUtil.verifyUserRole(featuregroupDTO, featurestore, user, project);
    try {
      featuregroupDTO = featuregroupController.
          deleteFeaturegroupIfExists(featurestore, featuregroupId, project, user, null, null);
      activityFacade.persistActivity(ActivityFacade.DELETED_FEATUREGROUP + featuregroupDTO.getName(),
          project, user, ActivityFlag.SERVICE);
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving a preview of a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of SELECT * from featuregroup LIMIT 20
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @GET
  @Path("/{featuregroupId}/preview")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Preview feature data of a featuregroup",
      response = RowValueQueryResult.class,
      responseContainer = "List")
  public Response getFeatureGroupPreview(
      @Context SecurityContext sc, @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      FeaturegroupDTO featuregroupDTO =
          featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
      List<RowValueQueryResult> featuresPreview =
          featuregroupController.getFeaturegroupPreview(featuregroupDTO, featurestore, project, user);
      GenericEntity<List<RowValueQueryResult>> featuresdataGeneric =
          new GenericEntity<List<RowValueQueryResult>>(featuresPreview) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuresdataGeneric).build();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_PREVIEW_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_PREVIEW_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving the SQL create schema of a featuregroup with a specified id in a specified featurestore
   *
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of SELECT * from featuregroup LIMIT 20
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @GET
  @Path("/{featuregroupId}/schema")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the SQL schema of a featuregroup",
      response = RowValueQueryResult.class)
  public Response getFeatureGroupSchema(
      @Context
          SecurityContext sc,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      FeaturegroupDTO featuregroupDTO =
          featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
      RowValueQueryResult schema =
          featuregroupController.getSchema(featuregroupDTO, project, user, featurestore);
      GenericEntity<RowValueQueryResult> schemaGeneric =
          new GenericEntity<RowValueQueryResult>(schema) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemaGeneric).build();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE,
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA.getMessage(), e);
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for deleting the contents of the featuregroup.
   * As HopsHive do not support ACID transactions the way to delete the contents of a table is to drop the table and
   * re-create it, which also will drop the featuregroup metadata due to ON DELETE CASCADE foreign key rule.
   * This method stores the metadata of the featuregroup before deleting it and then re-creates the featuregroup with
   * the same metadata.
   * <p>
   * This endpoint is typically used when the user wants to insert data into a featuregroup with the write-mode
   * 'overwrite' instead of default mode 'append'
   *
   * @param featuregroupId the id of the featuregroup
   * @return a JSON representation of the the featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featuregroupId}/clear")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete featuregroup contents",
      response = FeaturegroupDTO.class)
  public Response deleteFeaturegroupContents(
      @Context SecurityContext sc, @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //Verify that the user has the data-owner role or is the creator of the featuregroup
    FeaturegroupDTO oldFeaturegroupDTO = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featuregroupId);
    if(oldFeaturegroupDTO.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.CLEAR_OPERATION_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
          Level.FINE, "featuregroupId: " + featuregroupId);
    }
    featurestoreUtil.verifyUserRole(oldFeaturegroupDTO, featurestore, user, project);
    Jobs job = null;
    if (oldFeaturegroupDTO.getJobId() != null) {
      job = jobFacade.findByProjectAndId(project, oldFeaturegroupDTO.getJobId());
    }
    String featureStr = featurestoreUtil.makeCreateTableColumnsStr(oldFeaturegroupDTO.getFeatures(),
        oldFeaturegroupDTO.getDescription());
    try {
      featuregroupController.deleteFeaturegroupIfExists(featurestore, featuregroupId, project,
          user, null, null);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
    try {
      FeaturegroupDTO newFeaturegroupDTO = featuregroupController.createCachedFeaturegroup(project, user, featurestore,
          oldFeaturegroupDTO.getName(), featureStr,
          job, oldFeaturegroupDTO.getVersion(), oldFeaturegroupDTO.getFeatureCorrelationMatrix(),
          oldFeaturegroupDTO.getDescriptiveStatistics(), oldFeaturegroupDTO.getFeaturesHistogram(),
          oldFeaturegroupDTO.getClusterAnalysis());
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(newFeaturegroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for updating the featuregroup metadata without changing the schema.
   * Since the schema is not changed, the data does not need to be dropped.
   *
   * @param featuregroupId id of the featuregroup to update
   * @param featuregroupJsonDTO updated metadata
   * @return JSON representation of the updated featuregroup
   * @throws FeaturestoreException
   */
  @PUT
  @Path("/{featuregroupId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Update featuregroup contents",
      response = FeaturegroupDTO.class)
  public Response updateFeaturegroupMetadata(
      @Context SecurityContext sc, @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId, FeaturegroupJsonDTO featuregroupJsonDTO)
      throws FeaturestoreException {
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupJsonDTO.isUpdateStats() &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
            Settings.HOPS_FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    FeaturegroupDTO featuregroupDTO = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featuregroupId);
    featurestoreUtil.verifyUserRole(featuregroupDTO, featurestore, user, project);
    Jobs job = null;
    if (featuregroupJsonDTO.getJobName() != null && !featuregroupJsonDTO.getJobName().isEmpty()) {
      job = jobFacade.findByProjectAndName(project, featuregroupJsonDTO.getJobName());
    }
    FeaturegroupDTO updatedFeaturegroupDTO = featuregroupController.updateFeaturegroupMetadata(
        featurestore, featuregroupId, job,
        featuregroupJsonDTO.getFeatureCorrelationMatrix(), featuregroupJsonDTO.getDescriptiveStatistics(),
        featuregroupJsonDTO.isUpdateMetadata(), featuregroupJsonDTO.isUpdateStats(),
        featuregroupJsonDTO.getFeaturesHistogram(), featuregroupJsonDTO.getClusterAnalysis(),
        featuregroupJsonDTO.getName(), featuregroupJsonDTO.getDescription(), featuregroupJsonDTO.getJdbcConnectorId(),
        featuregroupJsonDTO.getSqlQuery(), featuregroupJsonDTO.getFeatures());
    activityFacade.persistActivity(ActivityFacade.EDITED_FEATUREGROUP +
        updatedFeaturegroupDTO.getName(), project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(updatedFeaturegroupDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
  }
}
