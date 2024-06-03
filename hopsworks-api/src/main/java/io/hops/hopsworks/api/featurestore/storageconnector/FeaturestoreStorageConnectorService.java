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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.StorageConnectorProvenanceResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.featurestore.storageconnectors.connectionChecker.ConnectionChecker;
import io.hops.hopsworks.common.featurestore.storageconnectors.connectionChecker.ConnectionCheckerDTO;
import io.hops.hopsworks.common.kafka.KafkaBrokers;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.persistence.entity.util.AbstractFacade;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.util.Set;
import java.util.logging.Level;

/**
 * A Stateless RESTful service for the storage connectors in a featurestore on Hopsworks.
 * Base URL: project/projectId/featurestores/featurestoreId/storageconnectors/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "StorageConnector service", description = "A service that manages a feature store's storage connectors")
public class FeaturestoreStorageConnectorService extends FeaturestoreSubResource {
  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturestoreStorageConnectorController storageConnectorController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private ConnectionChecker connectionChecker;
  @EJB
  private FeaturestoreStorageConnectorBuilder featurestoreStorageConnectorBuilder;
  @Inject
  private StorageConnectorProvenanceResource provenanceResource;

  @EJB
  private ProjectController projectController;

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }
  
  @Override
  protected FeaturestoreController getFeaturestoreController() {
    return featurestoreController;
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
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get all storage connectors of a feature store",
    response = FeaturestoreStorageConnectorDTO.class, responseContainer = "List")
  public Response getStorageConnectors(@BeanParam StorageConnectorBeanParam storageConnectorBeanParam,
                                       @Context SecurityContext sc,
                                       @Context HttpServletRequest req,
                                       @Context UriInfo uriInfo)
      throws FeaturestoreException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    ResourceRequest resourceRequest = makeResourceRequest(storageConnectorBeanParam);
    AbstractFacade.CollectionInfo<FeaturestoreConnector> connectors = storageConnectorController
        .findByFeaturestore(featurestore, resourceRequest.convertToQueryParam());
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
        featurestoreStorageConnectorBuilder.build(connectors, featurestore, project, user, resourceRequest, uriInfo);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreStorageConnectorDTO).build();
  }
  
  @GET
  @Path("{connectorName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get a storage connector with a specific name and from a featurestore",
    response = FeaturestoreStorageConnectorDTO.class)
  public Response getStorageConnector(@Context SecurityContext sc,
                                      @Context HttpServletRequest req,
                                      @ApiParam(value = "Name of the storage connector", required = true)
                                      @PathParam("connectorName") String connectorName)
      throws FeaturestoreException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    verifyStorageConnectorName(connectorName);
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
      storageConnectorController.getConnectorDTOWithName(user, project, featurestore, connectorName);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
      new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreStorageConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }
  
  /**
   * Endpoint for creating a storage connector with a particular type in a feature store
   *
   * @return a JSON representation of the connector
   * @throws FeaturestoreException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create a new storage connector for the feature store",
    response = FeaturestoreStorageConnectorDTO.class)
  public Response createNewStorageConnector(@Context SecurityContext sc,
                                            FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO)
      throws FeaturestoreException, UserException, ProjectException {
    if (featurestoreStorageConnectorDTO == null) {
      throw new IllegalArgumentException("Input JSON for creating a new Feature Store Storage Connector Cannot be " +
        "null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    FeaturestoreStorageConnectorDTO createdFeaturestoreStorageConnectorDTO = storageConnectorController
          .createStorageConnector(user, project, featurestore, featurestoreStorageConnectorDTO);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
      new GenericEntity<FeaturestoreStorageConnectorDTO>(createdFeaturestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED)
      .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }
  
  /**
   * Endpoint for deleting a storage connector with a particular type and name in a feature store
   *
   * @param connectorName
   *   the id of the storage connector
   * @return a JSON representation of the deleted connector
   * @throws FeaturestoreException
   */
  @DELETE
  @Path("{connectorName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete storage connector with a specific name and type from a featurestore")
  public Response deleteStorageConnector(@Context SecurityContext sc,
                                         @Context HttpServletRequest req,
                                         @ApiParam(value = "name of the storage connector", required = true)
                                         @PathParam("connectorName") String connectorName)
      throws UserException, FeaturestoreException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    storageConnectorController.deleteConnectorWithName(user, project, connectorName, featurestore);
    return Response.ok().build();
  }
  
  /**
   * Endpoint for updating a storage connector with a particular type and id in a feature store
   *
   * @param connectorName
   *   the id of the storage connector
   * @return a JSON representation of the updated connector
   * @throws FeaturestoreException
   */
  @PUT
  @Path("/{connectorName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a storage connector with a specific name and from a featurestore",
    response = FeaturestoreStorageConnectorDTO.class)
  public Response updateStorageConnector(@ApiParam(value = "Name of the storage connector", required = true)
                                         @PathParam("connectorName") String connectorName,
                                         FeaturestoreStorageConnectorDTO featurestoreStorageConnectorInputDTO,
                                         @Context SecurityContext sc)
      throws FeaturestoreException, UserException, ProjectException {
    if (featurestoreStorageConnectorInputDTO == null) {
      throw new IllegalArgumentException("Input JSON for updating a Feature Store Storage Connector Cannot be " +
        "null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    verifyStorageConnectorName(connectorName);
    storageConnectorController.updateStorageConnector(user, project, featurestore,
        featurestoreStorageConnectorInputDTO, connectorName);
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
        storageConnectorController.getConnectorDTOWithName(user, project, featurestore, connectorName);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
      new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }

  /**
   * This method returns the JDBC connector for the online featurestore for this user.
   * The JDBC connector is generated from the MySQL Server host:port, the user's username
   * and password (from the SecretsController).
   *
   * @param sc
   * @return
   * @throws FeaturestoreException
   */
  @GET
  @Path("/onlinefeaturestore")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get online featurestore storage connector for this feature store",
          response = FeaturestoreStorageConnectorDTO.class)
  public Response getOnlineFeaturestoreStorageConnector(@Context SecurityContext sc,
                                                        @Context HttpServletRequest req)
      throws FeaturestoreException, ProjectException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
              Level.FINE, "Online Featurestore is not enabled for this Hopsworks cluster.");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    FeaturestoreStorageConnectorDTO featurestoreJdbcConnectorDTO =
            storageConnectorController.getOnlineFeaturestoreConnector(user, project);
    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
            new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreJdbcConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
            .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }

  /**
   * This method returns the kafka connector for the featurestore.
   *
   * @param sc
   * @return
   * @throws FeaturestoreException
   */
  @GET
  @Path("/kafka_connector/byok")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get kafka storage connector for this feature store",
          response = FeaturestoreStorageConnectorDTO.class)
  public Response getFeatureStoreKafkaConnector(@Context SecurityContext sc,
                                                @Context HttpServletRequest req,
                                                @QueryParam("external") @DefaultValue("false")
                                                Boolean externalListeners)
      throws FeaturestoreException, ProjectException {
    KafkaBrokers.BrokerProtocol brokerProtocol = Boolean.TRUE.equals(externalListeners)
        ? KafkaBrokers.BrokerProtocol.EXTERNAL: KafkaBrokers.BrokerProtocol.INTERNAL;
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
            storageConnectorController.getKafkaConnector(getFeaturestore(), brokerProtocol);

    GenericEntity<FeaturestoreStorageConnectorDTO> featurestoreStorageConnectorDTOGenericEntity =
            new GenericEntity<FeaturestoreStorageConnectorDTO>(featurestoreStorageConnectorDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
            .entity(featurestoreStorageConnectorDTOGenericEntity).build();
  }

  /**
   * Endpoint for getting all enabled storage connector types in a featurestore
   *
   * @return a JSON representation of all the storage connector types in the feature store
   */
  @GET
  @Path("/types")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get storage connector types enabled",
          response = FeaturestoreConnectorType.class, responseContainer = "List")
  public Response getStorageConnectorTypes(@Context SecurityContext sc) {
    Set<FeaturestoreConnectorType> enabledStorageConnectorTypes =
            storageConnectorUtil.getEnabledStorageConnectorTypes();
    GenericEntity<Set<FeaturestoreConnectorType>> storageConnectorTypesGeneric =
            new GenericEntity<Set<FeaturestoreConnectorType>>(enabledStorageConnectorTypes) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(storageConnectorTypesGeneric)
            .build();
  }

  /**
   * Verify path parameters (storage connector id)
   *
   * @param connectorName
   *   id provided as a path parameter to a request
   */
  private void verifyStorageConnectorName(String connectorName) {
    if (Strings.isNullOrEmpty(connectorName)) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
  }
  
  /**
   * Test connection for storage connector
   *
   * @param sc
   * @param req
   * @param featurestoreStorageConnectorDTO
   * @return ConnectionCheckerDTO Output of connection test
   * @throws FeaturestoreException
   * @throws UserException
   * @throws ProjectException
   */
  @POST
  @Path("/testConnection")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Test storage connector to data source")
  public Response testConnection(@Context SecurityContext sc, @Context HttpServletRequest req,
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) throws FeaturestoreException, ProjectException {
    if (featurestoreStorageConnectorDTO == null) {
      throw new IllegalArgumentException("Input storage connector DTO cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    ConnectionCheckerDTO resultDto =
      connectionChecker.checkConnection(user, project, featurestore, featurestoreStorageConnectorDTO);
    
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(resultDto).build();
  }

  @Path("/{connectorName}/provenance")
  public StorageConnectorProvenanceResource provenance(
        @ApiParam(value = "Name of the storage connector", required = true)
        @PathParam("connectorName") String connectorName) {
    this.provenanceResource.setProjectId(getProjectId());
    this.provenanceResource.setFeaturestoreId(getFeaturestoreId());
    this.provenanceResource.setConnectorName(connectorName);
    return this.provenanceResource;
  }

  private ResourceRequest makeResourceRequest(StorageConnectorBeanParam param) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.STORAGECONNECTORS);
    resourceRequest.setOffset(param.getPagination().getOffset());
    resourceRequest.setLimit(param.getPagination().getLimit());
    resourceRequest.setSort(param.getParsedSortBy());
    resourceRequest.setFilter(param.getFilters());
    return resourceRequest;
  }
}
