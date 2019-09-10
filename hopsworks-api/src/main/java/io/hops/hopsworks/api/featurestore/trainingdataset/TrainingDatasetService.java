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

import io.hops.hopsworks.api.featurestore.util.FeaturestoreUtil;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsUpdateOperations;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorFacade;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetController;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset.HopsfsTrainingDatasetDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.logging.Level;

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
  private FeaturestoreUtil featurestoreUtil;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DsUpdateOperations dsUpdateOperations;
  @EJB
  private FeaturestoreHopsfsConnectorFacade featurestoreHopsfsConnectorFacade;

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
   * Endpoint for getting a list of all training datasets in the feature store.
   *
   * @return a JSON representation of the training datasets in the features store
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of training datasets for a featurestore",
      response = TrainingDatasetDTO.class, responseContainer = "List")
  public Response getTrainingDatasetsForFeaturestore() {
    List<TrainingDatasetDTO> trainingDatasetDTOs =
        trainingDatasetController.getTrainingDatasetsForFeaturestore(featurestore);
    GenericEntity<List<TrainingDatasetDTO>> trainingDatasetsGeneric =
        new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTOs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetsGeneric).build();
  }

  /**
   * Endpoint for creating a new trainingDataset
   *
   * @param trainingDatasetDTO the JSON payload with the data of the new trainingDataset
   * @return JSON representation of the created trainingDataset
   * @throws DatasetException
   * @throws HopsSecurityException
   * @throws ProjectException
   * @throws FeaturestoreException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create training dataset for a featurestore",
      response = TrainingDatasetDTO.class)
  public Response createTrainingDataset(@Context SecurityContext sc, TrainingDatasetDTO trainingDatasetDTO)
    throws DatasetException, HopsSecurityException, ProjectException, FeaturestoreException {
    if(trainingDatasetDTO == null){
      throw new IllegalArgumentException("Input JSON for creating a new Training Dataset cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO createdTrainingDatasetDTO = null;
    if(trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET){
      HopsfsTrainingDatasetDTO hopsfsTrainingDatasetDTO = (HopsfsTrainingDatasetDTO) trainingDatasetDTO;
      if(hopsfsTrainingDatasetDTO.getHopsfsConnectorId() == null) {
        throw new IllegalArgumentException(
          RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_ID_NOT_PROVIDED.getMessage());
      }
      FeaturestoreHopsfsConnector featurestoreHopsfsConnector = featurestoreHopsfsConnectorFacade.find(
        hopsfsTrainingDatasetDTO.getHopsfsConnectorId());
      if(featurestoreHopsfsConnector == null){
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
          Level.FINE, "hopsfsConnector: " + hopsfsTrainingDatasetDTO.getHopsfsConnectorId());
      }
      Dataset trainingDatasetsFolder = featurestoreHopsfsConnector.getHopsfsDataset();
      String trainingDatasetDirectoryName = trainingDatasetController.getTrainingDatasetPath(
        inodeFacade.getPath(trainingDatasetsFolder.getInode()),
        hopsfsTrainingDatasetDTO.getName(), hopsfsTrainingDatasetDTO.getVersion());
      org.apache.hadoop.fs.Path fullPath = null;
      try {
        fullPath = dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
          hopsfsTrainingDatasetDTO.getDescription(), -1, true);
      } catch (DatasetException e) {
        if (e.getErrorCode() == RESTCodes.DatasetErrorCode.DATASET_SUBDIR_ALREADY_EXISTS) {
          dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
          fullPath = dsUpdateOperations.createDirectoryInDataset(project, user, trainingDatasetDirectoryName,
            hopsfsTrainingDatasetDTO.getDescription(), -1, true);
        } else {
          throw e;
        }
      }
      Inode inode = inodeFacade.getInodeAtPath(fullPath.toString());
      hopsfsTrainingDatasetDTO.setInodeId(inode.getId());
      createdTrainingDatasetDTO = trainingDatasetController.createTrainingDataset(user, featurestore,
        hopsfsTrainingDatasetDTO);
    } else {
      createdTrainingDatasetDTO = trainingDatasetController.createTrainingDataset(user, featurestore,
        trainingDatasetDTO);
    }
    activityFacade.persistActivity(ActivityFacade.CREATED_TRAINING_DATASET +
        createdTrainingDatasetDTO.getName(), project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> createdTrainingDatasetDTOGeneric =
        new GenericEntity<TrainingDatasetDTO>(createdTrainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(createdTrainingDatasetDTOGeneric)
      .build();
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
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response getTrainingDatasetWithId(@ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid) throws FeaturestoreException {
    verifyIdProvided(trainingdatasetid);
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
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response deleteTrainingDataset(
      @Context SecurityContext sc, @ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid) throws FeaturestoreException, DatasetException,
      ProjectException, HopsSecurityException {
    verifyIdProvided(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(
        featurestore, trainingdatasetid);
    featurestoreUtil.verifyUserRole(trainingDatasetDTO, featurestore, user, project);
    if(trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET) {
      Dataset trainingDatasetsFolder = trainingDatasetController.getTrainingDatasetFolder(featurestore.getProject());
      String trainingDatasetDirectoryName = trainingDatasetController.getTrainingDatasetPath(
        inodeFacade.getPath(trainingDatasetsFolder.getInode()),
        trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
      dsUpdateOperations.deleteDatasetFile(project, user, trainingDatasetDirectoryName);
    } else {
      trainingDatasetController.deleteTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    }
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
   * @param trainingDatasetDTO the JSON payload with the new metadat
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
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Update a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(
      @Context SecurityContext sc, @ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid,
    @ApiParam(value = "updateMetadata", example = "true", defaultValue = "false")
    @QueryParam("updateMetadata") Boolean updateMetadata,
    @ApiParam(value = "updateStats", example = "true", defaultValue = "false")
    @QueryParam("updateStats") Boolean updateStats,
    TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, DatasetException, ProjectException, HopsSecurityException {
    if(trainingDatasetDTO == null){
      throw new IllegalArgumentException("Input JSON for updating a Training Dataset cannot be null");
    }
    updateMetadata = featurestoreUtil.updateMetadataGetOrDefault(updateMetadata);
    updateStats = featurestoreUtil.updateStatsGetOrDefault(updateStats);
    verifyIdProvided(trainingdatasetid);
    trainingDatasetDTO.setId(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO oldTrainingDatasetDTO =
      trainingDatasetController.getTrainingDatasetWithIdAndFeaturestore(featurestore, trainingdatasetid);
    if(updateMetadata){
      if (trainingDatasetDTO.getTrainingDatasetType() == TrainingDatasetType.HOPSFS_TRAINING_DATASET &&
        !oldTrainingDatasetDTO.getName().equals(trainingDatasetDTO.getName())) {
        Inode inode =
          trainingDatasetController.getInodeWithTrainingDatasetIdAndFeaturestore(featurestore, trainingdatasetid);
        Dataset trainingDatasetsFolder = trainingDatasetController.getTrainingDatasetFolder(featurestore.getProject());
        String trainingDatasetDirectoryName = trainingDatasetController.getTrainingDatasetPath(
          inodeFacade.getPath(trainingDatasetsFolder.getInode()),
          trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
        org.apache.hadoop.fs.Path fullPath =
          dsUpdateOperations.moveDatasetFile(project, user, inode, trainingDatasetDirectoryName);
        Inode newInode = inodeFacade.getInodeAtPath(fullPath.toString());
        HopsfsTrainingDatasetDTO hopsfsTrainingDatasetDTO = (HopsfsTrainingDatasetDTO) trainingDatasetDTO;
        hopsfsTrainingDatasetDTO.setInodeId(newInode.getId());
        TrainingDatasetDTO newTrainingDatasetDTO = trainingDatasetController.createTrainingDataset(user,
          featurestore, hopsfsTrainingDatasetDTO);
        return persistAndReturnEditedTrainingDatsetActivity(newTrainingDatasetDTO, user);
      } else {
        TrainingDatasetDTO updatedTrainingDatasetDTO = trainingDatasetController.updateTrainingDatasetMetadata(
          featurestore, trainingDatasetDTO);
        return persistAndReturnEditedTrainingDatsetActivity(updatedTrainingDatasetDTO, user);
      }
    }
    if(updateStats) {
      TrainingDatasetDTO updatedTrainingDatasetDTO = trainingDatasetController.updateTrainingDatasetStats(featurestore,
        trainingDatasetDTO);
      return persistAndReturnEditedTrainingDatsetActivity(updatedTrainingDatasetDTO, user);
    }
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGenericEntity =
      new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetDTOGenericEntity)
      .build();
  }
  
  /**
   * Verify that the user id was provided as a path param
   *
   * @param trainingDatasetId the training dataset id to verify
   */
  private void verifyIdProvided(Integer trainingDatasetId) {
    if (trainingDatasetId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }
  }
  
  /**
   * Persist the activity of editing a training dataset and return the response to the client
   *
   * @param trainingDatasetDTO DTO of the update training dataset
   * @param user the user making the request
   * @return JSON response of the DTO
   */
  private Response persistAndReturnEditedTrainingDatsetActivity(TrainingDatasetDTO trainingDatasetDTO, Users user) {
    activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + trainingDatasetDTO.getName(),
      project, user, ActivityFlag.SERVICE);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
      new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
  }

}
