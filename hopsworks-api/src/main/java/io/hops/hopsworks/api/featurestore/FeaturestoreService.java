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

import io.hops.hopsworks.api.featurestore.featuregroup.FeaturegroupService;
import io.hops.hopsworks.api.featurestore.storageconnector.FeaturestoreStorageConnectorService;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetService;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.app.FeaturestoreMetadataDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetController;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
  private ProjectFacade projectFacade;
  @Inject
  private FeaturegroupService featuregroupService;
  @Inject
  private TrainingDatasetService trainingDatasetService;
  @Inject
  private FeaturestoreStorageConnectorService featurestoreStorageConnectorService;

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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoresGeneric).build();
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
  @ApiOperation(value = "Get featurestore settings",
    response = FeaturestoreClientSettingsDTO.class)
  public Response getFeaturestoreSettings() {
    GenericEntity<FeaturestoreClientSettingsDTO> featurestoreClientSettingsDTO =
      new GenericEntity<FeaturestoreClientSettingsDTO>(new FeaturestoreClientSettingsDTO()) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreClientSettingsDTO).build();
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
  @ApiOperation(value = "Get featurestore with specific name",
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
        new GenericEntity<FeaturestoreMetadataDTO>(featurestoreMetadataDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(featurestoreMetadataGeneric)
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

}
