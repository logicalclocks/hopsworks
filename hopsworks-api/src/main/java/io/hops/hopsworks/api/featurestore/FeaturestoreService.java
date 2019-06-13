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

import io.hops.hopsworks.api.featurestore.featuregroups.FeaturegroupService;
import io.hops.hopsworks.api.featurestore.json.HopsfsConnectorJsonDTO;
import io.hops.hopsworks.api.featurestore.json.JdbcConnectorJsonDTO;
import io.hops.hopsworks.api.featurestore.json.S3ConnectorJsonDTO;
import io.hops.hopsworks.api.featurestore.json.TrainingDatasetJsonDTO;
import io.hops.hopsworks.api.featurestore.util.FeaturestoreUtil;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsUpdateOperations;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.app.FeaturestoreMetadataDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs.FeaturestoreHopsfsConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.s3.FeaturestoreS3ConnectorController;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.s3.FeaturestoreS3ConnectorDTO;
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
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
import java.util.List;
import java.util.logging.Logger;

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
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private FeaturestoreS3ConnectorController featurestoreS3ConnectorController;
  @EJB
  private FeaturestoreHopsfsConnectorController featurestoreHopsfsConnectorController;
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
  @EJB
  private DatasetFacade datasetFacade;
  @Inject
  private FeaturegroupService featuregroupService;

  private Project project;

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreService.class.getName());

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
  @ApiOperation(value = "Get the list of feature stores for the project",
    response = FeaturestoreDTO.class,
    responseContainer = "List")
  public Response getFeaturestores() {
    List<FeaturestoreDTO> featurestores = featurestoreController.getFeaturestoresForProject(project);
    GenericEntity<List<FeaturestoreDTO>> featurestoresGeneric =
      new GenericEntity<List<FeaturestoreDTO>>(featurestores) {
      };
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
    @PathParam("featurestoreId")
      Integer featurestoreId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreDTOGeneric)
      .build();
  }

  /**
   * Endpoint for getting a featurestore by name. This method will be removed after HOPSWORKS-860.
   *
   * @param featurestoreName the name of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  @Path("/getByName/{featurestoreName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get featurestore with specific Id",
    response = FeaturestoreDTO.class)
  public Response getFeaturestoreByName(
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreName")
      String featurestoreName) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(featurestoreName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NAME_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO =
      featurestoreController.getFeaturestoreForProjectWithName(project, featurestoreName);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreDTOGeneric)
      .build();
  }

  /**
   * Feature Groups sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature groups service
   * @throws FeaturestoreException
   */
  @Path("/{featurestoreId}/featuregroups")
  public FeaturegroupService featuregroupService(@PathParam("featurestoreId") Integer featurestoreId)
      throws FeaturestoreException {
    featuregroupService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featuregroupService.setFeaturestoreId(featurestoreId);
    return featuregroupService;
  }

  /**
   * Endpoint for getting a list of all training datasets in the feature store.
   *
   * @param featurestoreId id of the featurestore to query
   * @return a JSON representation of the training datasets in the features store
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
    @PathParam("featurestoreId")
      Integer featurestoreId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    List<TrainingDatasetDTO> trainingDatasetDTOs =
      trainingDatasetController.getTrainingDatasetsForFeaturestore(featurestore);
    GenericEntity<List<TrainingDatasetDTO>> trainingDatasetsGeneric =
      new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTOs) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(trainingDatasetsGeneric)
      .build();
  }

  /**
   * Endpoint for getting a training dataset with a particular id
   *
   * @param featurestoreId id of the featurestorelinked to the training dataset
   * @param trainingdatasetid id of the training dataset to get
   * @return return a JSON representation of the training dataset with the given id
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{featurestoreId}/trainingdatasets/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
    response = TrainingDatasetDTO.class)
  public Response getTrainingDatasetForFeaturestore(
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the training dataset", required = true)
    @PathParam("trainingdatasetid")
      Integer trainingdatasetid
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
      new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(trainingDatasetGeneric)
      .build();
  }

  /**
   * Endpoint for creating a new trainingDataset
   *
   * @param featurestoreId the featurestore where to create the trainingDataset
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create training dataset for a featurestore",
    response = TrainingDatasetDTO.class)
  public Response createTrainingDataset(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId, TrainingDatasetJsonDTO trainingDatasetJsonDTO)
    throws FeaturestoreException, DatasetException, HopsSecurityException, ProjectException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingDatasetJsonDTO.getFeatureCorrelationMatrix() != null &&
      trainingDatasetJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
        Settings.HOPS_FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    Jobs job = null;
    if (trainingDatasetJsonDTO.getJobName() != null && !trainingDatasetJsonDTO.getJobName().isEmpty()) {
      job = jobFacade.findByProjectAndName(project, trainingDatasetJsonDTO.getJobName());
    }
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
        job, trainingDatasetJsonDTO.getVersion(),
        trainingDatasetJsonDTO.getDataFormat(), inode, trainingDatasetsFolder,
        trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
        trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
        trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.getClusterAnalysis());
    activityFacade.persistActivity(ActivityFacade.CREATED_TRAINING_DATASET + trainingDatasetDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGeneric =
      new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder
      (Response.Status.CREATED).entity(trainingDatasetDTOGeneric).build();
  }

  /**
   * Endpoint for deleting a training dataset, this will delete both the metadata and the data storage
   *
   * @param featurestoreId the id of the featurestore linked to the trainingDataset
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
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the training dataset", required = true)
    @PathParam("trainingdatasetid")
      Integer trainingdatasetid
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
    featurestoreUtil.verifyUserRole(trainingDatasetDTO, featurestore, user, project);

    Dataset trainingDatasetsFolder = featurestoreUtil.getTrainingDatasetFolder(featurestore.getProject());
    String trainingDatasetDirectoryName = featurestoreUtil.getTrainingDatasetPath(
      inodeFacade.getPath(trainingDatasetsFolder.getInode()),
      trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
    dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET + trainingDatasetDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
      new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(trainingDatasetGeneric)
      .build();
  }

  /**
   * Endpoint for updating the trainingDataset metadata
   *
   * @param featurestoreId the id of the featurestore linked to the trainingDataset
   * @param trainingdatasetid the id of the trainingDataset to update
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
    response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the training dataset", required = true)
    @PathParam("trainingdatasetid")
      Integer trainingdatasetid, TrainingDatasetJsonDTO trainingDatasetJsonDTO
  ) throws FeaturestoreException, DatasetException, ProjectException, HopsSecurityException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    if (trainingDatasetJsonDTO.isUpdateStats() &&
      trainingDatasetJsonDTO.getFeatureCorrelationMatrix() != null &&
      trainingDatasetJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
        Settings.HOPS_FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());

    Jobs job = null;
    if (trainingDatasetJsonDTO.getJobName() != null && !trainingDatasetJsonDTO.getJobName().isEmpty()) {
      job = jobFacade.findByProjectAndName(project, trainingDatasetJsonDTO.getJobName());
    }
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
          job, trainingDatasetJsonDTO.getVersion(),
          trainingDatasetJsonDTO.getDataFormat(), newInode, trainingDatasetsFolder,
          trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
          trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
          trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + newTrainingDatasetDTO.getName(),
        project, user, ActivityFlag.SERVICE);
      GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(newTrainingDatasetDTO) {
        };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(trainingDatasetGeneric)
        .build();
    } else {
      TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.updateTrainingDataset(featurestore,
        trainingdatasetid, job, trainingDatasetJsonDTO.getDataFormat(),
        trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
        trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
        trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.isUpdateMetadata(),
        trainingDatasetJsonDTO.isUpdateStats(), trainingDatasetJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + trainingDatasetDTO.getName(),
        project, user, ActivityFlag.SERVICE);
      GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {
        };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(trainingDatasetGeneric)
        .build();
    }
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
  public Response getFeaturestoreId(
    @PathParam("featurestoreName")
      String featurestoreName)
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
    FeaturestoreMetadataDTO featurestoreMetadataDTO =
      new FeaturestoreMetadataDTO(featurestoreDTO, featuregroups, trainingDatasets);
    GenericEntity<FeaturestoreMetadataDTO> featurestoreMetadataGeneric =
      new GenericEntity<FeaturestoreMetadataDTO>(featurestoreMetadataDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreMetadataGeneric)
      .build();
  }

  /**
   * Endpoint for creating a new JDBC connector in a featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param jdbcConnectorJsonDTO JSON payload for the new JDBC connector
   * @return JSON information about the created connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featurestoreId}/jdbcconnectors")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create a JDBC connector for the feature store",
    response = FeaturestoreJdbcConnectorDTO.class)
  public Response createJdbcConnector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId, JdbcConnectorJsonDTO jdbcConnectorJsonDTO)
    throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    featurestoreJdbcConnectorController.verifyUserInput(jdbcConnectorJsonDTO.getName(),
      jdbcConnectorJsonDTO.getDescription(), featurestore,
      StringUtils.join(jdbcConnectorJsonDTO.getJdbcArguments(), ","),
      jdbcConnectorJsonDTO.getJdbcConnectionString());
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO =
      featurestoreJdbcConnectorController.createFeaturestoreJdbcConnector(jdbcConnectorJsonDTO.getName(),
        jdbcConnectorJsonDTO.getDescription(), featurestore,
        StringUtils.join(jdbcConnectorJsonDTO.getJdbcArguments(), ","),
        jdbcConnectorJsonDTO.getJdbcConnectionString());
    activityFacade.persistActivity(ActivityFacade.ADDED_JDBC_CONNECTOR + featurestoreJdbcConnectorDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreJdbcConnectorDTO> featurestoreJdbcDTOGenericEntity =
      new GenericEntity<FeaturestoreJdbcConnectorDTO>(featurestoreJdbcConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featurestoreJdbcDTOGenericEntity)
      .build();
  }

  /**
   * Endpoint for deleting a JDBC connector with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param jdbcConnectorId id of the jdbc connector
   * @return JSON representation of the deleted JDBC connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featurestoreId}/jdbcconnectors/{jdbcConnectorId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a specific JDBC connector",
    response = FeaturestoreJdbcConnectorDTO.class)
  public Response deleteJdbcConnector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the JDBC connector", required = true)
    @PathParam("jdbcConnectorId")
      Integer jdbcConnectorId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (jdbcConnectorId == null) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    FeaturestoreJdbcConnectorDTO featurestoreJdbcConnectorDTO =
      featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(jdbcConnectorId);
    activityFacade.persistActivity(ActivityFacade.REMOVED_JDBC_CONNECTOR +
        featurestoreJdbcConnectorDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreJdbcConnectorDTO> featurestoreJdbcDTOGenericEntity =
      new GenericEntity<FeaturestoreJdbcConnectorDTO>(featurestoreJdbcConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreJdbcDTOGenericEntity)
      .build();
  }

  /**
   * Endpoint for creating a new S3 connector in a featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param s3ConnectorJsonDTO JSON payload for the new S3 connector
   * @return JSON information about the created connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featurestoreId}/s3connectors")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create a S3 connector for the feature store",
    response = FeaturestoreS3ConnectorDTO.class)
  public Response createS3Connector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId, S3ConnectorJsonDTO s3ConnectorJsonDTO)
    throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    featurestoreS3ConnectorController.verifyUserInput(featurestore, s3ConnectorJsonDTO.getS3AccessKey(),
      s3ConnectorJsonDTO.getS3SecretKey(), s3ConnectorJsonDTO.getS3Bucket(),
      s3ConnectorJsonDTO.getDescription(), s3ConnectorJsonDTO.getName());
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO =
      featurestoreS3ConnectorController.createFeaturestoreS3Connector(featurestore, s3ConnectorJsonDTO.getS3AccessKey(),
        s3ConnectorJsonDTO.getS3SecretKey(), s3ConnectorJsonDTO.getS3Bucket(),
        s3ConnectorJsonDTO.getDescription(), s3ConnectorJsonDTO.getName());
    activityFacade.persistActivity(ActivityFacade.ADDED_S3_CONNECTOR + featurestoreS3ConnectorDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreS3ConnectorDTO> featurestoreS3DTOGenericEntity =
      new GenericEntity<FeaturestoreS3ConnectorDTO>(featurestoreS3ConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featurestoreS3DTOGenericEntity)
      .build();
  }

  /**
   * Endpoint for deleting a S3 connector with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param s3ConnectorId id of the S3 connector
   * @return JSON representation of the deleted S3 connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featurestoreId}/s3connectors/{s3ConnectorId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a specific S3 connector",
    response = FeaturestoreS3ConnectorDTO.class)
  public Response deleteS3Connector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the S3 connector", required = true)
    @PathParam("s3ConnectorId")
      Integer s3ConnectorId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (s3ConnectorId == null) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO =
      featurestoreS3ConnectorController.removeFeaturestoreS3Connector(s3ConnectorId);
    activityFacade.persistActivity(ActivityFacade.REMOVED_S3_CONNECTOR + featurestoreS3ConnectorDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreS3ConnectorDTO> featurestoreS3DTOGenericEntity =
      new GenericEntity<FeaturestoreS3ConnectorDTO>(featurestoreS3ConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreS3DTOGenericEntity)
      .build();
  }

  /**
   * Endpoint for creating a new HopsFS connector in a featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param hopsfsConnectorJsonDTO JSON payload for the new HopsFS connector
   * @return JSON information about the created connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @POST
  @Path("/{featurestoreId}/hopsfsconnectors")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create a HopsFS connector connector for the feature store",
    response = FeaturestoreHopsfsConnectorDTO.class)
  public Response createHopsfsConnector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId, HopsfsConnectorJsonDTO hopsfsConnectorJsonDTO)
    throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    Dataset dataset = datasetFacade.findByNameAndProjectId(project,
      hopsfsConnectorJsonDTO.getHopsFsDataset());
    featurestoreHopsfsConnectorController.verifyUserInput(featurestore, dataset,
      hopsfsConnectorJsonDTO.getDescription(), hopsfsConnectorJsonDTO.getName());
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO =
      featurestoreHopsfsConnectorController.createFeaturestoreHopsfsConnector(featurestore, dataset,
        hopsfsConnectorJsonDTO.getDescription(), hopsfsConnectorJsonDTO.getName());
    activityFacade.persistActivity(ActivityFacade.ADDED_HOPSFS_CONNECTOR + hopsfsConnectorJsonDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreHopsfsConnectorDTO> featurestoreHopsfsDTOGenericEntity =
      new GenericEntity<FeaturestoreHopsfsConnectorDTO>(featurestoreHopsfsConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(featurestoreHopsfsDTOGenericEntity)
      .build();
  }

  /**
   * Endpoint for deleting a HopsFS connector with a specified id in a specified featurestore
   *
   * @param featurestoreId id of the featurestore
   * @param hopsfConnectorId id of the Hopsfs connector
   * @return JSON representation of the deleted HopsFS connector
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @DELETE
  @Path("/{featurestoreId}/hopsfsconnectors/{hopsfConnectorId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a specific JDBC connector",
    response = FeaturestoreHopsfsConnectorDTO.class)
  public Response deleteHopsfsConnector(
    @Context
      SecurityContext sc,
    @ApiParam(value = "Id of the featurestore", required = true)
    @PathParam("featurestoreId")
      Integer featurestoreId,
    @ApiParam(value = "Id of the HopsFS connector", required = true)
    @PathParam("hopsfConnectorId")
      Integer hopsfConnectorId) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    if (hopsfConnectorId == null) {
      throw new IllegalArgumentException(
        RESTCodes.FeaturestoreErrorCode.STORAGE_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    //This call verifies that the project have access to the featurestoreId provided
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
    FeaturestoreHopsfsConnectorDTO featurestoreHopsfsConnectorDTO =
      featurestoreHopsfsConnectorController.removeFeaturestoreHopsfsConnector(hopsfConnectorId);
    activityFacade.persistActivity(ActivityFacade.REMOVED_HOPSFS_CONNECTOR +
        featurestoreHopsfsConnectorDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<FeaturestoreHopsfsConnectorDTO> featurestoreHopsfsDTOGenericEntity =
      new GenericEntity<FeaturestoreHopsfsConnectorDTO>(featurestoreHopsfsConnectorDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(featurestoreHopsfsDTOGenericEntity)
      .build();
  }

}
