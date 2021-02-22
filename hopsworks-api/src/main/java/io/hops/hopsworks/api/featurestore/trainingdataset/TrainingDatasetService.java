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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordResource;
import io.hops.hopsworks.api.featurestore.FsQueryBuilder;
import io.hops.hopsworks.api.featurestore.activities.ActivityResource;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.featurestore.tag.TagsBuilder;
import io.hops.hopsworks.api.featurestore.tag.TagsDTO;
import io.hops.hopsworks.api.featurestore.tag.TagsExpansionBeanParam;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.ProvArtifactResource;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.query.FsQueryDTO;
import io.hops.hopsworks.common.featurestore.tag.AttachTagResult;
import io.hops.hopsworks.common.featurestore.query.ServingPreparedStatementDTO;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
  private ActivityFacade activityFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private TagsBuilder tagBuilder;
  @Inject
  private FeatureStoreTagControllerIface tagController;
  @Inject
  private StatisticsResource statisticsResource;
  @EJB
  private FsQueryBuilder fsQueryBuilder;
  @Inject
  private ProvArtifactResource provenanceResource;
  @EJB
  private DatasetController datasetController;
  @Inject
  private FeaturestoreKeywordResource featurestoreKeywordResource;
  @Inject
  private ActivityResource activityResource;
  @EJB
  private FsJobManagerController fsJobManagerController;
  @EJB
  private JobsBuilder jobsBuilder;
  @EJB
  private PreparedStatementBuilder preparedStatementBuilder;

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
   * Sets the featurestore of the training datasets (provided by parent resource)
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
  public Response getAll(@Context SecurityContext sc) throws ServiceException, FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<TrainingDatasetDTO> trainingDatasetDTOs =
        trainingDatasetController.getTrainingDatasetsForFeaturestore(user, project, featurestore);
    GenericEntity<List<TrainingDatasetDTO>> trainingDatasetsGeneric =
        new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTOs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetsGeneric).build();
  }

  /**
   * Endpoint for creating a new trainingDataset
   *
   * @param trainingDatasetDTO the JSON payload with the data of the new trainingDataset
   * @return JSON representation of the created trainingDataset
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create training dataset for a featurestore",
      response = TrainingDatasetDTO.class)
  public Response create(@Context SecurityContext sc, TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {
    if(trainingDatasetDTO == null){
      throw new IllegalArgumentException("Input JSON for creating a new Training Dataset cannot be null");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO createdTrainingDatasetDTO =
        trainingDatasetController.createTrainingDataset(user, project, featurestore, trainingDatasetDTO);
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
   *
   * @deprecated : use getTrainingDatasetByName instead
   */
  @Deprecated
  @GET
  @Path("/{trainingdatasetid: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response getById(@ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid, @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException {
    verifyIdProvided(trainingdatasetid);

    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController
        .getTrainingDatasetWithIdAndFeaturestore(user, project, featurestore, trainingdatasetid);
    GenericEntity<TrainingDatasetDTO> trainingDatasetGeneric =
        new GenericEntity<TrainingDatasetDTO>(trainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetGeneric).build();
  }

  /**
   * Endpoint for getting a list of training dataset based on the name
   *
   * @param name name of the training dataset to get
   * @return return a JSON representation of the training dataset with the given id
   * @throws FeaturestoreException
   */
  @GET
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get a list of training datasets with a specific name, filter by version",
      response = List.class)
  public Response getByName(@ApiParam(value = "Name of the training dataset", required = true)
                                           @PathParam("name") String name,
                                           @ApiParam(value = "Filter by a specific version")
                                           @QueryParam("version") Integer version, @Context SecurityContext sc)
      throws FeaturestoreException, ServiceException {
    verifyNameProvided(name);

    Users user = jWTHelper.getUserPrincipal(sc);
    List<TrainingDatasetDTO> trainingDatasetDTO;
    if (version == null) {
      trainingDatasetDTO =
          trainingDatasetController.getWithNameAndFeaturestore(user, project, featurestore, name);
    } else {
      trainingDatasetDTO = Arrays.asList(trainingDatasetController
          .getWithNameVersionAndFeaturestore(user, project, featurestore, name, version));
    }

    GenericEntity<List<TrainingDatasetDTO>> trainingDatasetGeneric =
        new GenericEntity<List<TrainingDatasetDTO>>(trainingDatasetDTO) {};
    return Response.ok().entity(trainingDatasetGeneric).build();
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
  @Path("/{trainingdatasetid: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response delete(
      @Context SecurityContext sc, @ApiParam(value = "Id of the training dataset", required = true)
      @PathParam("trainingdatasetid") Integer trainingdatasetid)
      throws FeaturestoreException {
    verifyIdProvided(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);
    String trainingDsName = trainingDatasetController.delete(user, project, featurestore, trainingdatasetid);
    activityFacade.persistActivity(ActivityFacade.DELETED_TRAINING_DATASET + trainingDsName,
        project, user, ActivityFlag.SERVICE);
    return Response.ok().build();
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
  @Path("/{trainingdatasetid: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Update a training datasets with a specific id from a featurestore",
      response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(@Context SecurityContext sc,
                                        @ApiParam(value = "Id of the training dataset", required = true)
                                        @PathParam("trainingdatasetid") Integer trainingdatasetid,
                                        @ApiParam(value = "updateMetadata", example = "true")
                                        @QueryParam("updateMetadata") @DefaultValue("false") Boolean updateMetadata,
                                        @ApiParam(value = "updateStatsConfig", example = "true")
                                        @QueryParam("updateStatsConfig") @DefaultValue("false")
                                          Boolean updateStatsConfig,
                                        TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ServiceException {
    if(trainingDatasetDTO == null){
      throw new IllegalArgumentException("Input JSON for updating a Training Dataset cannot be null");
    }
    verifyIdProvided(trainingdatasetid);
    trainingDatasetDTO.setId(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO oldTrainingDatasetDTO = trainingDatasetController
        .getTrainingDatasetWithIdAndFeaturestore(user, project, featurestore, trainingdatasetid);

    if(updateMetadata){
      oldTrainingDatasetDTO =
          trainingDatasetController.updateTrainingDatasetMetadata(user, project, featurestore, trainingDatasetDTO);
      activityFacade.persistActivity(ActivityFacade.EDITED_TRAINING_DATASET + trainingDatasetDTO.getName(),
          project, user, ActivityFlag.SERVICE);
    }
    if (updateStatsConfig) {
      oldTrainingDatasetDTO =
          trainingDatasetController.updateTrainingDatasetStatsConfig(user, project, featurestore, trainingDatasetDTO);
    }
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGenericEntity =
      new GenericEntity<TrainingDatasetDTO>(oldTrainingDatasetDTO) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(trainingDatasetDTOGenericEntity)
      .build();
  }

  @ApiOperation( value = "Create or update tags(bulk) for a training dataset", response = TagsDTO.class)
  @PUT
  @Path("/{trainingdatasetId}/tags/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the training dataset", required = true)
                         @PathParam("trainingdatasetId") Integer trainingdatasetId,
                         @ApiParam(value = "Name of the tag", required = true)
                         @PathParam("name") String name,
                         @ApiParam(value = "Value to set for the tag") String value)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {

    verifyIdProvided(trainingdatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);

    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingdatasetId);
    AttachTagResult result = tagController.upsert(project, user, featurestore, trainingDataset, name, value);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
      featurestore.getId(), ResourceRequest.Name.TRAININGDATASETS.name(), trainingdatasetId, result.getItems());

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }

  @ApiOperation( value = "Create or update tags(bulk) for a training dataset", response = TagsDTO.class)
  @PUT
  @Path("/{trainingDatasetId}/tags")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response bulkPutTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                              @ApiParam(value = "Id of the training dataset", required = true)
                              @PathParam("trainingDatasetId") Integer trainingDatasetId,
                              TagsDTO tags)
    throws MetadataException, FeaturestoreException, FeatureStoreTagException {

    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    AttachTagResult result;

    if(tags.getItems().size() == 0) {
      result
        = tagController.upsert(project, user, featurestore, trainingDataset, tags.getName(), tags.getValue());
    } else {
      Map<String, String> newTags = new HashMap<>();
      for(TagsDTO tag : tags.getItems()) {
        newTags.put(tag.getName(), tag.getValue());
      }
      result = tagController.upsert(project, user, featurestore, trainingDataset, newTags);
    }

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
      featurestore.getId(), ResourceRequest.Name.TRAININGDATASETS.name(), trainingDatasetId, result.getItems());

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }

  @ApiOperation( value = "Get all tags attached to a training dataset", response = TagsDTO.class)
  @GET
  @Path("/{trainingDatasetId}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @ApiParam(value = "Id of the training dataset", required = true)
                          @PathParam("trainingDatasetId") Integer trainingDatasetId,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, FeaturestoreException, FeatureStoreTagException {

    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    Map<String, String> result = tagController.getAll(project, user, featurestore, trainingDataset);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
        featurestore.getId(), ResourceRequest.Name.TRAININGDATASETS.name(), trainingDatasetId, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Get tag attached to a training dataset", response = TagsDTO.class)
  @GET
  @Path("/{trainingDatasetId}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the training dataset", required = true)
                         @PathParam("trainingDatasetId") Integer trainingDatasetId,
                         @ApiParam(value = "Name of the tag", required = true)
                         @PathParam("name") String name,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, FeaturestoreException, FeatureStoreTagException {

    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    Map<String, String> result = tagController.get(project, user, featurestore,trainingDataset, name);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, project,
        featurestore.getId(), ResourceRequest.Name.TRAININGDATASETS.name(), trainingDatasetId, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Delete all attached tags to training dataset")
  @DELETE
  @Path("/{trainingDatasetId}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @ApiParam(value = "Id of the training dataset", required = true)
                             @PathParam("trainingDatasetId") Integer trainingDatasetId)
    throws DatasetException, MetadataException, FeaturestoreException {

    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    tagController.deleteAll(project, user, featurestore, trainingDataset);

    return Response.noContent().build();
  }

  @ApiOperation( value = "Delete tag attached to training dataset")
  @DELETE
  @Path("/{trainingDatasetId}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @ApiParam(value = "Id of the trainingdatasetid", required = true)
                            @PathParam("trainingDatasetId") Integer trainingDatasetId,
                            @ApiParam(value = "Name of the tag", required = true)
                            @PathParam("name") String name)
    throws DatasetException, MetadataException, FeaturestoreException {

    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    tagController.delete(project, user, featurestore, trainingDataset, name);

    return Response.noContent().build();
  }

  @Path("/{trainingDatasetId}/statistics")
  public StatisticsResource statistics(@PathParam("trainingDatasetId") Integer trainingDatsetId)
      throws FeaturestoreException {
    this.statisticsResource.setProject(project);
    this.statisticsResource.setFeaturestore(featurestore);
    this.statisticsResource.setTrainingDatasetId(trainingDatsetId);
    return statisticsResource;
  }

  @ApiOperation(value = "Get the query used to generated the training dataset", response = FsQueryDTO.class)
  @GET
  @Path("/{trainingdatasetid}/query")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getQuery(@Context SecurityContext sc,
                           @Context UriInfo uriInfo,
                           @ApiParam(value = "Id of the trainingdatasetid", required = true)
                           @PathParam("trainingdatasetid") Integer trainingdatasetid,
                           @ApiParam(value = "get query with label features", example = "true")
                           @QueryParam("withLabel") @DefaultValue("true") boolean withLabel)
      throws FeaturestoreException, ServiceException {
    verifyIdProvided(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);

    FsQueryDTO fsQueryDTO = fsQueryBuilder.build(uriInfo, project, user, featurestore, trainingdatasetid, withLabel);
    return Response.ok().entity(fsQueryDTO).build();
  }

  @POST
  @Path("/{trainingDatasetId}/compute")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Setup a job to compute and write a training dataset", response = JobDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response compute(@Context UriInfo uriInfo,
                          @Context SecurityContext sc,
                          @PathParam("trainingDatasetId") Integer trainingDatasetId,
                          TrainingDatasetJobConf trainingDatasetJobConf)
      throws FeaturestoreException, ServiceException, JobException, ProjectException, GenericException {
    verifyIdProvided(trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);

    Map<String, String> writeOptions = null;
    if (trainingDatasetJobConf.getWriteOptions() != null) {
      writeOptions = trainingDatasetJobConf.getWriteOptions()
          .stream().collect(Collectors.toMap(OptionDTO::getName, OptionDTO::getValue));
    }

    Jobs job = fsJobManagerController.setupTrainingDatasetJob(project, user, trainingDataset,
        trainingDatasetJobConf.getQuery(),
        trainingDatasetJobConf.getOverwrite(),
        writeOptions,
        trainingDatasetJobConf.getSparkJobConfiguration());
    JobDTO jobDTO = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);

    return Response.created(jobDTO.getHref()).entity(jobDTO).build();
  }

  @Path("/{trainingDatasetId}/provenance")
  public ProvArtifactResource provenance(@PathParam("trainingDatasetId") Integer trainingDatasetId)
    throws FeaturestoreException, GenericException {
    String tdName = featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
    Dataset targetEndpoint;
    try {
      targetEndpoint = datasetController.getByName(featurestore.getProject(), tdName);
    } catch (DatasetException ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "training dataset not found");
    }
    this.provenanceResource.setContext(project, targetEndpoint);
    TrainingDataset td = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    this.provenanceResource.setArtifactId(td.getName(), td.getVersion());
    return provenanceResource;
  }

  @Path("/{trainingDatasetId}/keywords")
  public FeaturestoreKeywordResource keywords (
      @ApiParam(value = "Id of the training dataset") @PathParam("trainingDatasetId") Integer trainingDatasetId)
      throws FeaturestoreException {
    this.featurestoreKeywordResource.setProject(project);
    this.featurestoreKeywordResource.setFeaturestore(featurestore);
    this.featurestoreKeywordResource.setTrainingDatasetId(trainingDatasetId);
    return featurestoreKeywordResource;
  }

  @Path("/{trainingDatasetId}/activity")
  public ActivityResource activity(@ApiParam(value = "Id of the training dataset")
                                   @PathParam("trainingDatasetId") Integer trainingDatasetId)
      throws FeaturestoreException {
    this.activityResource.setProject(project);
    this.activityResource.setFeaturestore(featurestore);
    this.activityResource.setTrainingDatasetId(trainingDatasetId);
    return this.activityResource;
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
   * Verify that the name was provided as a path param
   *
   * @param trainingDatasetName the training dataset id to verify
   */
  private void verifyNameProvided(String trainingDatasetName) {
    if (Strings.isNullOrEmpty(trainingDatasetName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.
          TRAINING_DATASET_NAME_NOT_PROVIDED.getMessage());
    }
  }

  @ApiOperation(value = "Get prepared statements used to generate model serving vector from training dataset query",
      response = ServingPreparedStatementDTO.class)
  @GET
  @Path("/{trainingdatasetid}/preparedstatements")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getQuery(@Context SecurityContext sc,
                           @Context UriInfo uriInfo,
                           @ApiParam(value = "Id of the trainingdatasetid", required = true)
                           @PathParam("trainingdatasetid") Integer trainingdatasetid)
      throws FeaturestoreException {
    verifyIdProvided(trainingdatasetid);
    Users user = jWTHelper.getUserPrincipal(sc);

    ServingPreparedStatementDTO servingPreparedStatementDTO = preparedStatementBuilder.build(uriInfo,
        new ResourceRequest(ResourceRequest.Name.PREPAREDSTATEMENTS), project, user, featurestore, trainingdatasetid);
    return Response.ok().entity(servingPreparedStatementDTO).build();
  }
}

