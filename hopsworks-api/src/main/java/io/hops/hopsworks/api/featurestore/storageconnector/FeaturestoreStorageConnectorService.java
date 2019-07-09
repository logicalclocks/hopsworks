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

package io.hops.hopsworks.api.featurestore.storageconnector;

import io.hops.hopsworks.api.featurestore.util.FeaturestoreUtil;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.exceptions.FeaturestoreException;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * A Stateless RESTful service for the storage connectors in a featurestore on Hopsworks.
 * Base URL: project/projectId/featurestores/featurestoreId/storageconnectors/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "StorageConnector service", description = "A service that manages a feature store's storage connectors")
public class FeaturestoreStorageConnectorService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturestoreStorageConnectorController featurestoreStorageConnectorController;
  @EJB
  private FeaturestoreUtil featurestoreUtil;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;
  private Featurestore featurestore;

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
   * Endpoint for getting all storage connectors in a featurestore
   *
   * @return a JSON representation of all the storage connectors in the feature store
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get all storage connectors of a feature store",
      response = FeaturestoreStorageConnectorDTO.class, responseContainer = "List")
  public Response getStorageConnectors() {
    List<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOS =
        featurestoreStorageConnectorController.getAllStorageConnectorsForFeaturestore(featurestore);
    GenericEntity<List<FeaturestoreStorageConnectorDTO>> featurestoreStorageConnectorsGeneric =
        new GenericEntity<List<FeaturestoreStorageConnectorDTO>>(featurestoreStorageConnectorDTOS) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreStorageConnectorsGeneric)
        .build();
  }

  /**
   * Endpoint for getting all storage connectors of a specific type in a featurestore
   *
   * @param connectorType type of the storage connector, e.g S3, JDBC or HopsFS
   * @return a JSON representation of all the storage connectors with the provided type in the feature store
   */
  @GET
  @Path("/{connectorType : JDBC|S3|HopsFS}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get all storage connectors of a specific type of a feature store",
      response = FeaturestoreStorageConnectorDTO.class, responseContainer = "List")
  public Response getStorageConnectorsOfType(
      @ApiParam (value = "storage connector type", example = "JDBC")
      @PathParam("connectorType") FeaturestoreStorageConnectorType connectorType) {
    if (connectorType == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_TYPE_NOT_PROVIDED.getMessage());
    }
    List<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOS =
        featurestoreStorageConnectorController.getAllStorageConnectorsForFeaturestoreWithType(featurestore,
            connectorType);
    GenericEntity<List<FeaturestoreStorageConnectorDTO>> featurestoreStorageConnectorsGeneric =
        new GenericEntity<List<FeaturestoreStorageConnectorDTO>>(featurestoreStorageConnectorDTOS) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreStorageConnectorsGeneric)
        .build();
  }

  /**
   * Endpoint for getting a storage connector with a particular type and id in a feature store
   *
   * @param connectorType type of the storage connector, e.g S3, JDBC or HopsFS
   * @param connectorId the id of the storage connector
   * @return a JSON representation of the connector
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{connectorType : JDBC|S3|HopsFS}/{connectorId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a storage connector with a specific id and type from a featurestore",
      response = FeaturestoreStorageConnectorDTO.class)
  public Response getStorageConnectorWithId(
      @ApiParam (value = "storage connector type", example = "JDBC", required=true)
      @PathParam("connectorType") FeaturestoreStorageConnectorType connectorType,
      @ApiParam(value = "Id of the storage connector", required = true)
      @PathParam("connectorId") Integer connectorId)
      throws FeaturestoreException {
    if (connectorType == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_TYPE_NOT_PROVIDED.getMessage());
    }
    if (connectorId == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
        featurestoreStorageConnectorController.getStorageConnectorForFeaturestoreWithTypeAndId(featurestore,
            connectorType, connectorId);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
        new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }

  /**
   * Endpoint for creating a storage connector with a particular type in a feature store
   *
   * @param connectorType type of the storage connector, e.g S3, JDBC or HopsFS
   * @return a JSON representation of the connector
   */
  @POST
  @Path("/{connectorType : JDBC|S3|HopsFS}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create a new storage connector for the feature store",
      response = FeaturestoreStorageConnectorDTO.class)
  public Response createNewStorageConnectorWithType(
      @Context SecurityContext sc,
      @ApiParam (value = "storage connector type", example = "JDBC", required=true)
      @PathParam("connectorType") FeaturestoreStorageConnectorType connectorType,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) {
    if (connectorType == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_TYPE_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    FeaturestoreStorageConnectorDTO createdFeaturestoreStorageConnectorDTO =
        featurestoreStorageConnectorController.createStorageConnectorWithType(featurestore, connectorType,
            featurestoreStorageConnectorDTO);
    activityFacade.persistActivity(ActivityFacade.ADDED_FEATURESTORE_STORAGE_CONNECTOR +
        featurestoreStorageConnectorDTO.getName(), project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
        new GenericEntity<FeaturestoreStorageConnectorDTO>(createdFeaturestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }

  /**
   * Endpoint for deleting a storage connector with a particular type and id in a feature store
   *
   * @param connectorType type of the storage connector, e.g S3, JDBC or HopsFS
   * @param connectorId the id of the storage connector
   * @return a JSON representation of the deleted connector
   * @throws FeaturestoreException
   */
  @DELETE
  @Path("/{connectorType : JDBC|S3|HopsFS}/{connectorId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete storage connector with a specific id and type from a featurestore",
      response = FeaturestoreStorageConnectorDTO.class)
  public Response deleteStorageConnectorWithTypeAndId(
      @Context SecurityContext sc,
      @ApiParam (value = "storage connector type", example = "JDBC", required=true)
      @PathParam("connectorType") FeaturestoreStorageConnectorType connectorType,
      @ApiParam(value = "Id of the storage connector", required = true)
      @PathParam("connectorId") Integer connectorId)
      throws FeaturestoreException {
    if (connectorType == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_TYPE_NOT_PROVIDED.getMessage());
    }
    if (connectorId == null) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
        featurestoreStorageConnectorController.getStorageConnectorForFeaturestoreWithTypeAndId(featurestore,
            connectorType, connectorId);
    featurestoreUtil.verifyUserRole(featurestore, user, project, featurestoreStorageConnectorDTO);
    featurestoreStorageConnectorDTO =
        featurestoreStorageConnectorController.deleteStorageConnectorWithTypeAndId(connectorType, connectorId);
    activityFacade.persistActivity(ActivityFacade.REMOVED_FEATURESTORE_STORAGE_CONNECTOR +
        featurestoreStorageConnectorDTO.getName(), project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
        new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }
}