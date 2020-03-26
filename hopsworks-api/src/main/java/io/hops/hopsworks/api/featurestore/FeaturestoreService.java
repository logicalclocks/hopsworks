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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.featurestore.featuregroup.FeaturegroupService;
import io.hops.hopsworks.api.featurestore.storageconnector.FeaturestoreStorageConnectorService;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetService;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.ImportControllerIface;
import io.hops.hopsworks.common.featurestore.app.FeaturestoreMetadataDTO;
import io.hops.hopsworks.common.featurestore.app.FeaturestoreUtilJobDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.importjob.FeaturegroupImportJobDTO;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasetjob.TrainingDatasetJobControllerIface;
import io.hops.hopsworks.common.featurestore.trainingdatasetjob.TrainingDatasetJobDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * A Stateless RESTful service for the featurestore service on Hopsworks.
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
  private ProjectFacade projectFacade;
  @EJB
  private FeaturestoreStorageConnectorController featurestoreStorageConnectorController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @Inject
  private FeaturegroupService featuregroupService;
  @Inject
  private TrainingDatasetService trainingDatasetService;
  @Inject
  private FeaturestoreStorageConnectorService featurestoreStorageConnectorService;
  @EJB
  private DataValidationResource dataValidationService;
  @Inject
  private ImportControllerIface importControllerIface;
  @Inject
  private TrainingDatasetJobControllerIface trainingDatasetJobControllerIface;
  @EJB
  private JobsBuilder jobsBuilder;
  @Inject
  private FsQueryConstructorResource fsQueryConstructorResource;

  private Project project;

  /**
   * Set the project of the featurestore (provided by parent resource)
   *
   * @param projectId the id of the project
   */
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of feature stores for the project",
    response = FeaturestoreDTO.class,
    responseContainer = "List")
  public Response getFeaturestores(@Context SecurityContext sc) throws FeaturestoreException {
    List<FeaturestoreDTO> featurestores = featurestoreController.getFeaturestoresForProject(project);
    GenericEntity<List<FeaturestoreDTO>> featurestoresGeneric =
      new GenericEntity<List<FeaturestoreDTO>>(featurestores) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoresGeneric).build();
  }

  /**
   * Endpoint for getting a featurestore with a particular Id
   *
   * @param featurestoreId the id of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  @Path("/{featurestoreId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore with specific Id",
    response = FeaturestoreDTO.class)
  public Response getFeaturestore(
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId") Integer featurestoreId, @Context SecurityContext sc) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreDTOGeneric).build();
  }

  /**
   * Endpoint for getting a featurestore's settings
   **
   * @return JSON representation of the featurestore settings
   */
  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore settings",
    response = FeaturestoreClientSettingsDTO.class)
  public Response getFeaturestoreSettings(@Context SecurityContext sc) {
    FeaturestoreClientSettingsDTO featurestoreClientSettingsDTO = new FeaturestoreClientSettingsDTO();
    featurestoreClientSettingsDTO.setFeaturestoreUtil4jExecutable("hdfs:///user" + org.apache.hadoop.fs.Path.SEPARATOR
      + settings.getSparkUser() + org.apache.hadoop.fs.Path.SEPARATOR
      + settings.getHopsExamplesFeaturestoreUtil4JFilename());
    featurestoreClientSettingsDTO.setFeaturestoreUtilPythonExecutable("hdfs:///user"
      + org.apache.hadoop.fs.Path.SEPARATOR
      + settings.getSparkUser() + org.apache.hadoop.fs.Path.SEPARATOR
      + settings.getHopsExamplesFeaturestoreUtilPythonFilename());
    featurestoreClientSettingsDTO.setOnlineFeaturestoreEnabled(settings.isOnlineFeaturestore());
    featurestoreClientSettingsDTO.setS3IAMRole(settings.isIAMRoleConfigured());
    GenericEntity<FeaturestoreClientSettingsDTO> featurestoreClientSettingsDTOGeneric =
      new GenericEntity<FeaturestoreClientSettingsDTO>(featurestoreClientSettingsDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreClientSettingsDTOGeneric)
      .build();
  }
  
  /**
   * Endpoint for getting a featurestore by name.
   *
   * @param name
   *   the name of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  // Anything else that is not just number should use this endpoint
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore with a specific name",
    response = FeaturestoreDTO.class)
  public Response getFeaturestoreByName(
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("name") String name, @Context SecurityContext sc) throws FeaturestoreException {
    verifyNameProvided(name);
    FeaturestoreDTO featurestoreDTO =
      featurestoreController.getFeaturestoreForProjectWithName(project, name);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreDTOGeneric).build();
  }

  /**
   * Endpoint for getting all metadata for a feature store. This is a convenience endpoint only used by program-clients
   * that needs feature store metadata for query planning. This endpoint means that the client do not have to do
   * 3 requests to get all metadata (featurestore, featuregroups, training datasets)
   *
   * @param featurestoreName featurestoreName
   * @return a JSON representation of the featurestore metadata
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{featurestoreName}/metadata")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore Metadata",
    response = FeaturestoreClientSettingsDTO.class)
  public Response getFeaturestoreId(@Context SecurityContext sc, @PathParam("featurestoreName") String featurestoreName)
    throws FeaturestoreException {
    if (Strings.isNullOrEmpty(featurestoreName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NAME_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO =
      featurestoreController.getFeaturestoreForProjectWithName(project, featurestoreName);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    List<FeaturegroupDTO> featuregroups = featuregroupController.getFeaturegroupsForFeaturestore(featurestore);
    List<TrainingDatasetDTO> trainingDatasets =
      trainingDatasetController.getTrainingDatasetsForFeaturestore(featurestore);
    List<FeaturestoreStorageConnectorDTO> storageConnectors =
      featurestoreStorageConnectorController.getAllStorageConnectorsForFeaturestore(featurestore);
    FeaturestoreJdbcConnectorDTO onlineFeaturestoreConnector = null;
    FeaturestoreClientSettingsDTO featurestoreClientSettingsDTO = new FeaturestoreClientSettingsDTO();
    featurestoreClientSettingsDTO.setOnlineFeaturestoreEnabled(settings.isOnlineFeaturestore());
    if(settings.isOnlineFeaturestore()
      && onlineFeaturestoreController.checkIfDatabaseExists(
          onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject()))) {
      Users user = jWTHelper.getUserPrincipal(sc);
      String dbUsername = onlineFeaturestoreController.onlineDbUsername(project, user);
      String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(project);
      onlineFeaturestoreConnector =
        featurestoreStorageConnectorController.getOnlineFeaturestoreConnector(user, project,
          dbUsername, featurestore, dbName);
      featurestoreDTO.setMysqlServerEndpoint(settings.getFeaturestoreJdbcUrl());
      featurestoreDTO.setOnlineFeaturestoreSize(onlineFeaturestoreController.getDbSize(
          onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject())));
      featurestoreDTO.setOnlineFeaturestoreType(FeaturestoreConstants.ONLINE_FEATURE_STORE_TYPE);
      featurestoreDTO.setOnlineFeaturestoreName(featurestore.getProject().getName());
      featurestoreDTO.setOnlineEnabled(true);
    }
    FeaturestoreMetadataDTO featurestoreMetadataDTO =
      new FeaturestoreMetadataDTO(featurestoreDTO, featuregroups, trainingDatasets,
        featurestoreClientSettingsDTO, storageConnectors, onlineFeaturestoreConnector);
    GenericEntity<FeaturestoreMetadataDTO> featurestoreMetadataGeneric =
      new GenericEntity<FeaturestoreMetadataDTO>(featurestoreMetadataDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreMetadataGeneric)
        .build();
  }
  
  
  @Path("{featureStoreId}/datavalidation")
  public DataValidationResource dataValidation(@PathParam("featureStoreId") Integer featureStoreId) {
    return this.dataValidationService.setFeatureStore(featureStoreId);
  }

  /**
   * Feature Groups sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature groups service
   * @throws FeaturestoreException
   */
  @Path("/{featurestoreId}/featuregroups")
  public FeaturegroupService featuregroupService(@PathParam("featurestoreId") Integer featurestoreId,
    @Context SecurityContext sc) throws FeaturestoreException {
    featuregroupService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featuregroupService.setFeaturestoreId(featurestoreId);
    return featuregroupService;
  }

  /**
   * Training Datasets sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the training dataset service
   * @throws FeaturestoreException
   */
  @Path("/{featurestoreId}/trainingdatasets")
  public TrainingDatasetService trainingDatasetService(@PathParam("featurestoreId") Integer featurestoreId)
      throws FeaturestoreException {
    trainingDatasetService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    trainingDatasetService.setFeaturestoreId(featurestoreId);
    return trainingDatasetService;
  }

  /**
   * Storage Connectors sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the storage connector service
   * @throws FeaturestoreException
   */
  @Path("/{featurestoreId}/storageconnectors")
  public FeaturestoreStorageConnectorService storageConnectorService(
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    featurestoreStorageConnectorService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featurestoreStorageConnectorService.setFeaturestoreId(featurestoreId);
    return featurestoreStorageConnectorService;
  }

  /**
   * Endpoint for uploading job-arguments to hdfs for featurestore utility jobs
   **
   * @return HDFS path to the uploaded arguments
   */
  @POST
  @Path("/util")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Upload json input for featurestore-util jobs")
  public Response newFeaturestoreUtil(@Context SecurityContext sc, FeaturestoreUtilJobDTO featurestoreUtilJobDTO)
      throws FeaturestoreException, JAXBException {
    if(featurestoreUtilJobDTO == null){
      throw new IllegalArgumentException("Input JSON for creating a new Feature Store Util Job cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    String hdfsPath = featurestoreController.writeUtilArgsToHdfs(user, project, featurestoreUtilJobDTO);
    JsonResponse jsonResponse = noCacheResponse.buildJsonResponse(Response.Status.OK, hdfsPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jsonResponse).build();
  }


  /**
   * Endpoint for creating a job to import a feature group from external sources
   *
   * @param featuregroupImportJobDTO JSON to configure the job
   * @return
   */
  @Path("/importjob")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Configure job to import featuregroup", response = JobDTO.class)
  public Response createOrUpdateImportJob(@Context SecurityContext sc, @Context UriInfo uriInfo,
      @ApiParam(value = "Job configuration", required = true) FeaturegroupImportJobDTO featuregroupImportJobDTO)
      throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (featuregroupImportJobDTO == null) {
      throw new IllegalArgumentException("Job specification not provided");
    }
    if (Strings.isNullOrEmpty(featuregroupImportJobDTO.getFeaturegroup())) {
      throw new IllegalArgumentException("Featuregroup name not provided");
    }
    Jobs job = importControllerIface.createImportJob(user, project, featuregroupImportJobDTO);
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Integer.toString(dto.getId()));
    return Response.created(builder.build()).entity(dto).build();
  }
  
  /**
   * Endpoint for creating a job to create a training dataset from a featurestore
   *
   * @param trainingDatasetJobDTO JSON to configure the job
   * @return
   */
  @Path("/trainingdatasetjob")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Configure job to create training dataset", response = JobDTO.class)
  public Response createOrUpdateTrainingDatasetJob(@Context SecurityContext sc, @Context UriInfo uriInfo,
    @ApiParam(value = "Job configuration", required = true) TrainingDatasetJobDTO trainingDatasetJobDTO)
    throws FeaturestoreException, JAXBException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (trainingDatasetJobDTO == null) {
      throw new IllegalArgumentException("Job specification not provided");
    }
    if (Strings.isNullOrEmpty(trainingDatasetJobDTO.getTrainingDataset())) {
      throw new IllegalArgumentException("Training dataset name not provided");
    }
    Jobs job = trainingDatasetJobControllerIface.createTrainingDatasetJob(user, project, trainingDatasetJobDTO);
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Integer.toString(dto.getId()));
    return Response.created(builder.build()).entity(dto).build();
  }

  @Path("/query")
  public FsQueryConstructorResource constructQuery() {
    return fsQueryConstructorResource.setProject(project);
  }

  /**
   * Verify that the name was provided as a path param
   *
   * @param featureStoreName the feature store name to verify
   */
  private void verifyNameProvided(String featureStoreName) {
    if (Strings.isNullOrEmpty(featureStoreName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NAME_NOT_PROVIDED.getMessage());
    }
  }
}
