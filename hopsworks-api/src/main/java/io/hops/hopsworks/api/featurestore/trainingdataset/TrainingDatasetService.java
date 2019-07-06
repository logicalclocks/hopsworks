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

package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.api.featurestore.trainingdataset.json.TrainingDatasetJsonDTO;
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
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetController;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
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
import java.util.List;
import java.util.logging.Logger;

/**
 * A Stateless RESTful service for the training datasets in a featurestore on Hopsworks.
 * Base URL: project/projectId/featurestores/featurestoreId/trainingdatasets/
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "TrainingDataset service", description = "A service that manages a feature store's training datasets")
public class TrainingDatasetService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private FeaturestoreUtil featurestoreUtil;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DsUpdateOperations dsUpdateOperations;

  private Project project;
  private Featurestore featurestore;
  private static final Logger LOGGER = Logger.getLogger(TrainingDatasetService.class.getName());

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
   * Endpoint for getting a list of all training datasets in the feature store.
   *
   * @return a JSON representation of the training datasets in the features store
   * @throws FeaturestoreException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of training datasets for a featurestore",
      response = TrainingDatasetDTO.class, responseContainer = "List")
  public Response getTrainingDatasetsForFeaturestore() throws FeaturestoreException {
    List<TrainingDatasetDTO> trainingDatasetDTOs =
        trainingDatasetController.getTrainingDatasetsForFeaturestore(featurestore);
    GenericEntity<List<TrainingDatasetDTO>> trainingDatasetsGeneric =
        new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTOs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetsGeneric).build();
  }

  /**
   * Endpoint for creating a new trainingDataset
   *
   * @param trainingDatasetJsonDTO the JSON payload with the data of the new trainingDataset
   * @return JSON representation of the created trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws HopsSecurityException
   * @throws ProjectException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create training dataset for a featurestore",
      response = TrainingDatasetDTO.class)
  public Response createTrainingDataset(@Context SecurityContext sc, TrainingDatasetJsonDTO trainingDatasetJsonDTO)
      throws FeaturestoreException, DatasetException, HopsSecurityException, ProjectException {
    if (trainingDatasetJsonDTO.getFeatureCorrelationMatrix() != null &&
        trainingDatasetJsonDTO.getFeatureCorrelationMatrix().getFeatureCorrelations().size() >
            Settings.HOPS_FEATURESTORE_STATISTICS_MAX_CORRELATIONS) {
      throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.CORRELATION_MATRIX_EXCEED_MAX_SIZE.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
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
      fullPath = dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
          trainingDatasetJsonDTO.getDescription(), -1, true);
    } catch (DatasetException e) {
      if (e.getErrorCode() == RESTCodes.DatasetErrorCode.DATASET_SUBDIR_ALREADY_EXISTS) {
        dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
        fullPath = dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
            trainingDatasetJsonDTO.getDescription(), -1, true);
      } else {
        throw e;
      }
    }
    Inode inode = inodeFacade.getInodeAtPath(fullPath.toString());
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.createTrainingDataset(project, user, featurestore,
        job, trainingDatasetJsonDTO.getVersion(),
        trainingDatasetJsonDTO.getDataFormat(), inode, trainingDatasetsFolder,
        trainingDatasetJsonDTO.getDescription(), trainingDatasetJsonDTO.getFeatureCorrelationMatrix(),
        trainingDatasetJsonDTO.getDescriptiveStatistics(), trainingDatasetJsonDTO.getFeaturesHistogram(),
        trainingDatasetJsonDTO.getFeatures(), trainingDatasetJsonDTO.getClusterAnalysis());
    activityFacade.persistActivity(ActivityFacade.CREATED_TRAINING_DATASET + trainingDatasetDTO.getName(),
        project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(trainingDatasetDTOGeneric).build();
  }

  /**
   * Endpoint for getting a training dataset with a particular id
   *
   * @param trainingdatasetid id of the training dataset to get
   * @return return a JSON representation of the training dataset with the given id
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response getTrainingDatasetWithId(@ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid) throws FeaturestoreException {
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    TrainingDatasetDTO trainingDatasetDTO =
        trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
  }

  /**
   * Endpoint for deleting a training dataset, this will delete both the metadata and the data storage
   *
   * @param trainingdatasetid the id of the trainingDataset
   * @return JSON representation of the deleted trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws ProjectException
   */
  @DELETE
  @Path("/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response deleteTrainingDataset(
      @Context SecurityContext sc, @ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid) throws FeaturestoreException, DatasetException,
      ProjectException, HopsSecurityException {
    if (trainingdatasetid == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(
        featurestore, trainingdatasetid);
    featurestoreUtil.verifyUserRole(trainingDatasetDTO, featurestore, user, project);
    Dataset trainingDatasetsFolder = featurestoreUtil.getTrainingDatasetFolder(featurestore.getProject());
    String trainingDatasetDirectoryName = featurestoreUtil.getTrainingDatasetPath(
        inodeFacade.getPath(trainingDatasetsFolder.getInode()),
        trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
    dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET + trainingDatasetDTO.getName(),
        project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
  }

  /**
   * Endpoint for updating the metadata of a training dataset
   *
   * @param trainingdatasetid the id of the trainingDataset to update
   * @param trainingDatasetJsonDTO the JSON payload with the new metadat
   * @return JSON representation of the updated trainingDataset
   * @throws FeaturestoreException
   * @throws DatasetException
   * @throws ProjectException
   */
  @PUT
  @Path("/{trainingdatasetid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Update a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(
      @Context SecurityContext sc, @ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid, TrainingDatasetJsonDTO trainingDatasetJsonDTO)
      throws FeaturestoreException, DatasetException, ProjectException, HopsSecurityException {
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
      TrainingDatasetDTO newTrainingDatasetDTO = trainingDatasetController.createTrainingDataset(project, user,
          featurestore, job, trainingDatasetJsonDTO.getVersion(), trainingDatasetJsonDTO.getDataFormat(), newInode,
          trainingDatasetsFolder, trainingDatasetJsonDTO.getDescription(),
          trainingDatasetJsonDTO.getFeatureCorrelationMatrix(), trainingDatasetJsonDTO.getDescriptiveStatistics(),
          trainingDatasetJsonDTO.getFeaturesHistogram(), trainingDatasetJsonDTO.getFeatures(),
          trainingDatasetJsonDTO.getClusterAnalysis());
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + newTrainingDatasetDTO.getName(),
          project, user, ActivityFlag.SERVICE);
      GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
          new GenericEntity<TrainingDatasetDTO>(newTrainingDatasetDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
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
          new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
    }
  }

}
