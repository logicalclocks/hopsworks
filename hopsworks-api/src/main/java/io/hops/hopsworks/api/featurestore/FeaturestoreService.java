/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore;

import io.hops.hopsworks.api.featurestore.json.FeaturegroupJsonDTO;
import io.hops.hopsworks.api.featurestore.json.TrainingDatasetJsonDTO;
import io.hops.hopsworks.api.featurestore.util.FeaturestoreUtil;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsUpdateOperations;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.RowValueQueryResult;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetController;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.FeaturestoreException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
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
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A Stateless RESTful service for the featurestore microservice on Hopsworks.
 * Base URL: project/id/featurestores/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Featurestore service", description = "A service that manages project's feature stores")
public class FeaturestoreService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private FeaturestoreUtil featurestoreUtil;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DsUpdateOperations dsUpdateOperations;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreService.class.getName());

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  /**
   * Endpoint for getting the list of featurestores for the project
   *
   * @return list of featurestore in JSON representation
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of feature stores for the project",
      response = FeaturestoreDTO.class,
      responseContainer = "List")
  public Response getFeaturestores() {
    List<FeaturestoreDTO> featurestores = featurestoreController.getFeaturestoresForProject(project);
    GenericEntity<List<FeaturestoreDTO>> featurestoresGeneric =
        new GenericEntity<List<FeaturestoreDTO>>(featurestores) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoresGeneric)
        .build();
  }

  /**
   * Endpoint for getting a featurestore with a particular Id
   *
   * @param featurestoreId the id of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  @Path("/{featurestoreId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore with specific Id",
      response = FeaturestoreDTO.class)
  public Response getFeaturestore(
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
        new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreDTOGeneric)
        .build();
  }

  /**
   * Endpoint for getting all featuregroups of a featurestore
   *
   * @param featurestoreId id of the featurestore
   * @return list of JSON featuregroups
   */
  @GET
  @Path("/{featurestoreId}/featuregroups")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of feature groups for a featurestore",
      response = FeaturegroupDTO.class,
      responseContainer = "List")
  public Response getFeaturegroupsForFeaturestore(
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    List<FeaturegroupDTO> featuregroups = featuregroupController.
        getFeaturegroupsForFeaturestore(featurestore);
    GenericEntity<List<FeaturegroupDTO>> featuregroupsGeneric =
        new GenericEntity<List<FeaturegroupDTO>>(featuregroups) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featuregroupsGeneric)
        .build();
  }

  /**
   * Endpoint for creating a new featuregroup in a featurestore
   *
   * @param featurestoreId      id of the featuregroup
   * @param featuregroupJsonDTO JSON payload for the new featuregroup
   * @return JSON information about the created featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featurestoreId}/featuregroups")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create feature group for a featurestore",
      response = FeaturegroupDTO.class,
      responseContainer = "List")
  public Response createFeaturegroup(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId, FeaturegroupJsonDTO featuregroupJsonDTO)
      throws FeaturestoreException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupJsonDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() > 50) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    Jobs job = null;
    if (featuregroupJsonDTO.getJobName() != null)
      job = jobFacade.findByProjectAndName(project, featuregroupJsonDTO.getJobName());
    String featureStr = featurestoreUtil.makeCreateTableColumnsStr(featuregroupJsonDTO.getFeatures());
    try {
      featuregroupController.dropFeaturegroup(featuregroupJsonDTO.getName(),
          featuregroupJsonDTO.getVersion(), project, user, featurestore);
      FeaturegroupDTO featuregroupDTO = featuregroupController.createFeaturegroup(project, user, featurestore,
          featuregroupJsonDTO.getName(), featureStr, featuregroupJsonDTO.getDescription(),
          featuregroupJsonDTO.getDependencies(), job, featuregroupJsonDTO.getVersion(),
          featuregroupJsonDTO.getFeatureCorrelationMatrix(), featuregroupJsonDTO.getDescriptiveStatistics(),
          featuregroupJsonDTO.getFeaturesHistogram(), featuregroupJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.CREATED_FEATUREGROUP + featuregroupDTO.getName(),
          project, user, ActivityFacade.ActivityFlag.SERVICE);
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featuregroupGeneric).build();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving a featuregroup with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the featuregroup
   */
  @GET
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get specific featuregroup from a specific featurestore",
      response = FeaturegroupDTO.class)
  public Response getFeatureGroupFromFeatureStore(
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    FeaturegroupDTO featuregroupDTO =
        featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
    GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featuregroupGeneric)
        .build();
  }

  /**
   * Endpoint for deleting a featuregroup with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of the deleted featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete specific featuregroup from a specific featurestore",
      response = FeaturegroupDTO.class)
  public Response deleteFeatureGroupFromFeatureStore(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    try {
      FeaturegroupDTO featuregroupDTO = featuregroupController.
          deleteFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId, project, user);
      activityFacade.persistActivity(ActivityFacade.DELETED_FEATUREGROUP + featuregroupDTO.getName(),
          project, user, ActivityFacade.ActivityFlag.SERVICE);
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(featuregroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(featuregroupGeneric)
          .build();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving a preview of a featuregroup with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of SELECT * from featuregroup LIMIT 20
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @GET
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}/preview")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Preview feature data of a featuregroup",
      response = RowValueQueryResult.class,
      responseContainer = "List")
  public Response getFeatureGroupPreview(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      //This call verifies that the project have access to the featurestoreId provided
      FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
      Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
      FeaturegroupDTO featuregroupDTO =
          featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
      List<RowValueQueryResult> featuresPreview =
          featuregroupController.getFeaturegroupPreview(featuregroupDTO, featurestore, project, user);
      GenericEntity<List<RowValueQueryResult>> featuresdataGeneric =
          new GenericEntity<List<RowValueQueryResult>>(featuresPreview) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(featuresdataGeneric)
          .build();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_PREVIEW_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_PREVIEW_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for retrieving the SQL create schema of a featuregroup with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param featuregroupId id of the featuregroup
   * @return JSON representation of SELECT * from featuregroup LIMIT 20
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @GET
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}/schema")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the SQL schema of a featuregroup",
      response = RowValueQueryResult.class)
  public Response getFeatureGroupSchema(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      //This call verifies that the project have access to the featurestoreId provided
      FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
      Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
      FeaturegroupDTO featuregroupDTO =
          featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
      RowValueQueryResult schema =
          featuregroupController.getSchema(featuregroupDTO, project, user, featurestore);
      GenericEntity<RowValueQueryResult> schemaGeneric =
          new GenericEntity<RowValueQueryResult>(schema) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(schemaGeneric)
          .build();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE,
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA.getMessage(), e);
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId +
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
   * This endpoint is typically used when the user wants to insert data into a featuregroup with the write-mode \
   * 'overwrite' instead of default mode 'append'
   *
   * @param featurestoreId the id of the featurestore
   * @param featuregroupId the id of the featuregroup
   * @return a JSON representation of the the featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}/clear")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete featuregroup contents",
      response = FeaturegroupDTO.class)
  public Response deleteFeaturegroupContents(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId) throws FeaturestoreException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    FeaturegroupDTO oldFeaturegroupDTO =
        featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId);
    Jobs job = null;
    if (oldFeaturegroupDTO.getJobId() != null)
      job = jobFacade.findByProjectAndId(project, oldFeaturegroupDTO.getJobId());
    String featureStr = featurestoreUtil.makeCreateTableColumnsStr(oldFeaturegroupDTO.getFeatures());
    try {
      featuregroupController.deleteFeaturegroupWithIdAndFeaturestore(featurestore, featuregroupId, project, user);
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
    try {
      List<String> dependencies = oldFeaturegroupDTO.getDependencies().stream().map(d ->
          d.getPath()).collect(Collectors.toList());
      FeaturegroupDTO newFeaturegroupDTO = featuregroupController.createFeaturegroup(project, user, featurestore,
          oldFeaturegroupDTO.getName(), featureStr, oldFeaturegroupDTO.getDescription(),
          dependencies,
          job, oldFeaturegroupDTO.getVersion(), oldFeaturegroupDTO.getFeatureCorrelationMatrix(),
          oldFeaturegroupDTO.getDescriptiveStatistics(), oldFeaturegroupDTO.getFeaturesHistogram(),
          oldFeaturegroupDTO.getClusterAnalysis());
      GenericEntity<FeaturegroupDTO> featuregroupGeneric =
          new GenericEntity<FeaturegroupDTO>(newFeaturegroupDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "project: " + project.getName() + ", featurestoreId: " + featurestoreId +
              ", featuregroupId: " + featuregroupId, e.getMessage(), e);
    }
  }

  /**
   * Endpoint for updating the featuregroup metadata without changing the schema.
   * Since the schema is not changed, the data does not need to be dropped.
   *
   * @param featurestoreId      id of the featurestore where the featuregroup resides
   * @param featuregroupId      id of the featuregroup to update
   * @param featuregroupJsonDTO updated metadata
   * @return JSON representation of the updated featuregroup
   * @throws FeaturestoreException
   */
  @PUT
  @Path("/{featurestoreId}/featuregroups/{featuregroupId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete featuregroup contents",
      response = FeaturegroupDTO.class)
  public Response updateFeaturegroupMetadata(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("featuregroupId") Integer featuregroupId,
      FeaturegroupJsonDTO featuregroupJsonDTO) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_ID_NOT_PROVIDED.getMessage());
    }
    if (featuregroupJsonDTO.isUpdateStats() &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix() != null &&
        featuregroupJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() > 50) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    Jobs job = null;
    if (featuregroupJsonDTO.getJobName() != null && !featuregroupJsonDTO.getJobName().isEmpty())
      job = jobFacade.findByProjectAndName(project, featuregroupJsonDTO.getJobName());
    FeaturegroupDTO updatedFeaturegroupDTO = featuregroupController.updateFeaturegroupMetadata(
        featurestore, featuregroupId, job, featuregroupJsonDTO.getDependencies(),
        featuregroupJsonDTO.getFeatureCorrelationMatrix(), featuregroupJsonDTO.getDescriptiveStatistics(),
        featuregroupJsonDTO.isUpdateMetadata(), featuregroupJsonDTO.isUpdateStats(),
        featuregroupJsonDTO.getFeaturesHistogram(), featuregroupJsonDTO.getClusterAnalysis());
    activityFacade.persistActivity(ActivityFacade.EDITED_FEATUREGROUP +
        updatedFeaturegroupDTO.getName(), project, user, ActivityFacade.ActivityFlag.SERVICE);
    GenericEntity<FeaturegroupDTO> featuregroupGeneric =
        new GenericEntity<FeaturegroupDTO>(updatedFeaturegroupDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featuregroupGeneric).build();
  }

  /**
   * Endpoint for getting a list of all training datasets in the feature store.
   *
   * @param featurestoreId id of the featurestore to query
   * @return
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{featurestoreId}/trainingdatasets")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of training datasets for a featurestore",
      response = TrainingDatasetDTO.class,
      responseContainer = "List")
  public Response getTrainingDatasetsForFeaturestore(
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    List<TrainingDatasetDTO> trainingDatasetDTOs =
        trainingDatasetController.getTrainingDatasetsForFeaturestore(featurestore);
    GenericEntity<List<TrainingDatasetDTO>> featuregroupsGeneric =
        new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTOs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featuregroupsGeneric)
        .build();
  }

  /**
   * Endpoint for getting a training dataset with a particular id
   *
   * @param featurestoreId    id of the featurestorelinked to the training dataset
   * @param trainingdatasetid id of the training dataset to get
   * @return
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{featurestoreId}/trainingdatasets/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response getTrainingDatasetForFeaturestore(
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid
  ) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    TrainingDatasetDTO trainingDatasetDTO =
        trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(trainingDatasetGeneric)
        .build();
  }

  /**
   * Endpoint for creating a new trainingDataset
   *
   * @param featurestoreId         the featurestore where to create the trainingDataset
   * @param trainingDatasetJsonDTO the JSON payload with the data of the new trainingDataset
   * @return JSON representation of the created trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws HopsSecurityException
   * @throws ProjectException
   */
  @POST
  @Path("/{featurestoreId}/trainingdatasets")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create training dataset for a featurestore",
      response = TrainingDatasetDTO.class)
  public Response createTrainingDataset(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId, TrainingDatasetJsonDTO trainingDatasetJsonDTO)
      throws FeaturestoreException, DatasetException, HopsSecurityException, ProjectException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingDatasetJsonDTO.getFeatureCorrelationMatrix() != null &&
        trainingDatasetJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() > 50) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    Jobs job = null;
    if (trainingDatasetJsonDTO.getJobName() != null && !trainingDatasetJsonDTO.getJobName().isEmpty())
      job = jobFacade.findByProjectAndName(project, trainingDatasetJsonDTO.getJobName());
    Dataset trainingDatasetsFolder = featurestoreUtil.getTrainingDatasetFolder(featurestore.getProject());
    String trainingDatasetDirectoryName = featurestoreUtil.getTrainingDatasetPath(
        inodeFacade.getPath(trainingDatasetsFolder.getInode()),
        trainingDatasetJsonDTO.getName(), trainingDatasetJsonDTO.getVersion());
    org.apache.hadoop.fs.Path fullPath = null;
    try {
      fullPath =
          dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
              trainingDatasetJsonDTO.getDescription(),
              -1, true);
    } catch (DatasetException e) {
      if (e.getErrorCode() == RESTCodes.DatasetErrorCode.DATASET_SUBDIR_ALREADY_EXISTS) {
        dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
        fullPath =
            dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
                trainingDatasetJsonDTO.getDescription(),
                -1, true);
      } else {
        throw e;
      }
    }
    Inode inode = inodeFacade.getInodeAtPath(fullPath.toString());
    TrainingDatasetDTO trainingDatasetDTO =
        trainingDatasetController.createTrainingDataset(project, user, featurestore,
            trainingDatasetJsonDTO.getDependencies(), job, trainingDatasetJsonDTO.getVersion(),
            trainingDatasetJsonDTO.getDataFormat(), inode, trainingDatasetsFolder,
            trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
            trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
            trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.getClusterAnalysis());
    activityFacade.persistActivity(ActivityFacade.CREATED_TRAINING_DATASET + trainingDatasetDTO.getName(), project,
      user, ActivityFacade.ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder
        (Response.Status.CREATED).entity(trainingDatasetDTOGeneric).build();
  }

  /**
   * Endpoint for deleting a training dataset, this will delete both the metadata and the data storage
   *
   * @param featurestoreId    the id of the featurestore linked to the trainingDataset
   * @param trainingdatasetid the id of the trainingDataset
   * @return JSON representation of the deleted trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws ProjectException
   */
  @DELETE
  @Path("/{featurestoreId}/trainingdatasets/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response deleteTrainingDataset(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid
  ) throws FeaturestoreException, DatasetException, ProjectException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    TrainingDatasetDTO trainingDatasetDTO =
        trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    Dataset trainingDatasetsFolder = featurestoreUtil.getTrainingDatasetFolder(featurestore.getProject());
    String trainingDatasetDirectoryName = featurestoreUtil.getTrainingDatasetPath(
        inodeFacade.getPath(trainingDatasetsFolder.getInode()),
        trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
    dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET + trainingDatasetDTO.getName(),
        project, user, ActivityFacade.ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(trainingDatasetGeneric)
        .build();
  }

  /**
   * Endpoint for updating the trainingDataset metadata
   *
   * @param featurestoreId         the id of the featurestore linked to the trainingDataset
   * @param trainingdatasetid      the id of the trainingDataset to update
   * @param trainingDatasetJsonDTO the JSON payload with the new metadat
   * @return JSON representation of the updated trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws ProjectException
   */
  @PUT
  @Path("/{featurestoreId}/trainingdatasets/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(
      @Context SecurityContext sc,
      @ApiParam(value = "Id of the featurestore", required = true)
      @PathParam("featurestoreId") Integer featurestoreId,
      @ApiParam(value = "Id of the featuregroup", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid, TrainingDatasetJsonDTO trainingDatasetJsonDTO
  ) throws FeaturestoreException, DatasetException, ProjectException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingDatasetJsonDTO.isUpdateStats() &&
        trainingDatasetJsonDTO.getFeatureCorrelationMatrix() != null &&
        trainingDatasetJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() > 50) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());

    Jobs job = null;
    if (trainingDatasetJsonDTO.getJobName() != null && !trainingDatasetJsonDTO.getJobName().isEmpty())
      job = jobFacade.findByProjectAndName(project, trainingDatasetJsonDTO.getJobName());
    TrainingDatasetDTO oldTrainingDatasetDTO =
        trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    if (!oldTrainingDatasetDTO.getName().equals(trainingDatasetJsonDTO.getName())) {
      Inode inode =
          trainingDatasetController.getInodeWithTrainingDatasetIdAndFeaturestore(featurestore, trainingdatasetid);
      Dataset trainingDatasetsFolder = featurestoreUtil.getTrainingDatasetFolder(featurestore.getProject());
      String trainingDatasetDirectoryName = featurestoreUtil.getTrainingDatasetPath(
          inodeFacade.getPath(trainingDatasetsFolder.getInode()),
          trainingDatasetJsonDTO.getName(), trainingDatasetJsonDTO.getVersion());
      org.apache.hadoop.fs.Path fullPath =
          dsUpdateOperations.moveDatasetFile(project, user, inode, trainingDatasetDirectoryName);
      Inode newInode = inodeFacade.getInodeAtPath(fullPath.toString());
      TrainingDatasetDTO newTrainingDatasetDTO =
          trainingDatasetController.createTrainingDataset(project, user, featurestore,
              trainingDatasetJsonDTO.getDependencies(), job, trainingDatasetJsonDTO.getVersion(),
              trainingDatasetJsonDTO.getDataFormat(), newInode, trainingDatasetsFolder,
              trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
              trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
              trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + newTrainingDatasetDTO.getName(),
          project, user, ActivityFacade.ActivityFlag.SERVICE);
      GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
          new GenericEntity<TrainingDatasetDTO>(newTrainingDatasetDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(trainingDatasetGeneric)
          .build();
    } else {
      TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.updateTrainingDataset(featurestore,
          trainingdatasetid, job, trainingDatasetJsonDTO.getDependencies(), trainingDatasetJsonDTO.getDataFormat(),
          trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
          trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
          trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.isUpdateMetadata(),
          trainingDatasetJsonDTO.isUpdateStats(), trainingDatasetJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + trainingDatasetDTO.getName(),
          project, user, ActivityFacade.ActivityFlag.SERVICE);
      GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
          new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
          .entity(trainingDatasetGeneric)
          .build();
    }
  }
}