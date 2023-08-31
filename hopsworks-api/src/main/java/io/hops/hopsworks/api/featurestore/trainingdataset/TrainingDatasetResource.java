/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordResource;
import io.hops.hopsworks.api.featurestore.tag.TrainingDatasetTagResource;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.TrainingDatasetProvenanceResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetResource {

  @Inject
  private StatisticsResource statisticsResource;
  @Inject
  private TrainingDatasetTagResource tagResource;
  @Inject
  private FeaturestoreKeywordResource featurestoreKeywordResource;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FsJobManagerController fsJobManagerController;
  @EJB
  private JobsBuilder jobsBuilder;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private TrainingDatasetDTOBuilder trainingDatasetDTOBuilder;
  @Inject
  private TrainingDatasetProvenanceResource provenanceResource;

  private Featurestore featurestore;
  private FeatureView featureView;
  private Project project;

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Create a training dataset.", response = TrainingDatasetDTO.class)
  public Response create(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ProvenanceException, IOException, ServiceException {
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDatasetDTO createdTrainingDatasetDTO =
        trainingDatasetController.createTrainingDataset(user, project, featurestore, featureView, trainingDatasetDTO);
    return Response.created(uriInfo.getRequestUri()).entity(createdTrainingDatasetDTO).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get the list of training datasets.",
      response = TrainingDatasetDTO.class, responseContainer = "List")
  public Response getAll(
      @Context
          SecurityContext sc,
      @Context
          UriInfo uriInfo,
      @Context
          HttpServletRequest req,
      @BeanParam
          TrainingDatasetExpansionBeanParam param
  ) throws FeaturestoreException, ServiceException, MetadataException, DatasetException, FeatureStoreMetadataException,
           IOException {
    Users user = jWTHelper.getUserPrincipal(sc);
    List<TrainingDataset> trainingDatasets =
        trainingDatasetController.getTrainingDatasetByFeatureView(featureView);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetDTOBuilder.build(user, project, trainingDatasets,
        uriInfo, resourceRequest);
    return Response.ok().entity(trainingDatasetDTO).build();
  }

  @GET
  @Path("/version/{version: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get a training datasets with a specific name and version.", response = List.class)
  public Response getByVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @BeanParam
          TrainingDatasetExpansionBeanParam param,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException, ServiceException, MetadataException, DatasetException, FeatureStoreMetadataException,
           IOException {
    Users user = jWTHelper.getUserPrincipal(sc);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView,
        version);
    ResourceRequest resourceRequest = makeResourceRequest(param);
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetDTOBuilder.build(user, project, trainingDataset, uriInfo,
        resourceRequest);
    return Response.ok().entity(trainingDatasetDTO).build();
  }

  private ResourceRequest makeResourceRequest(TrainingDatasetExpansionBeanParam param) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TRAININGDATASETS);
    resourceRequest.setExpansions(param.getResources());
    return resourceRequest;
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete all training datasets.", response = TrainingDatasetDTO.class)
  public Response delete(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req
  ) throws FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    trainingDatasetController.delete(user, project, featurestore, featureView);
    return Response.ok().build();
  }

  @DELETE
  @Path("/version/{version: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete a specific version of training dataset .", response = TrainingDatasetDTO.class)
  public Response deleteByVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    trainingDatasetController.delete(user, project, featurestore, featureView, version);
    return Response.ok().build();
  }

  @DELETE
  @Path("/data")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete all data only of training datasets.", response = TrainingDatasetDTO.class)
  public Response deleteDataOnly(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req
  ) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    trainingDatasetController.deleteDataOnly(user, project, featurestore, featureView);
    return Response.ok().build();
  }

  @DELETE
  @Path("/version/{version: [0-9]+}/data")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Delete data only of a specific version of training dataset.",
      response = TrainingDatasetDTO.class)
  public Response deleteDataOnlyByVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    Users user = jWTHelper.getUserPrincipal(sc);
    trainingDatasetController.deleteDataOnly(user, project, featurestore, featureView, version);
    return Response.ok().build();
  }

  @PUT
  @Path("/version/{version: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Update a training dataset.", response = TrainingDatasetDTO.class)
  public Response updateTrainingDataset(@Context SecurityContext sc,
                                        @Context HttpServletRequest req,
                                        @ApiParam(value = "updateMetadata", example = "true")
                                        @QueryParam("updateMetadata") @DefaultValue("false")
                                            Boolean updateMetadata,
                                        @ApiParam(value = "updateStatsConfig", example = "true")
                                        @QueryParam("updateStatsConfig") @DefaultValue("false")
                                            Boolean updateStatsConfig,
                                        @PathParam("version")
                                            Integer version,
                                        TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException, ServiceException {
    if (trainingDatasetDTO == null) {
      throw new IllegalArgumentException("Input JSON for updating a Training Dataset cannot be null");
    }

    Users user = jWTHelper.getUserPrincipal(sc);
    trainingDatasetDTO.setVersion(version);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView,
        version);
    trainingDatasetDTO.setId(trainingDataset.getId());
    TrainingDatasetDTO oldTrainingDatasetDTO = trainingDatasetController.convertTrainingDatasetToDTO(user, project,
        trainingDataset);

    if (updateMetadata) {
      oldTrainingDatasetDTO =
          trainingDatasetController.updateTrainingDatasetMetadata(user, project, featurestore, trainingDatasetDTO);
    }
    if (updateStatsConfig) {
      oldTrainingDatasetDTO =
          trainingDatasetController.updateTrainingDatasetStatsConfig(user, project, featurestore, trainingDatasetDTO);
    }
    GenericEntity<TrainingDatasetDTO> trainingDatasetDTOGenericEntity =
        new GenericEntity<TrainingDatasetDTO>(oldTrainingDatasetDTO) {};
    return Response.ok().entity(trainingDatasetDTOGenericEntity).build();
  }

  @Path("/version/{version: [0-9]+}/keywords")
  public FeaturestoreKeywordResource keywords(
      @ApiParam(value = "Version of the training dataset", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    this.featurestoreKeywordResource.setProject(project);
    this.featurestoreKeywordResource.setFeaturestore(featurestore);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(
        featureView, version);
    this.featurestoreKeywordResource.setTrainingDataset(trainingDataset);
    return this.featurestoreKeywordResource;
  }

  @Path("/version/{version: [0-9]+}/statistics")
  public StatisticsResource statistics(
      @ApiParam(value = "Version of the training dataset", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    statisticsResource.setProject(project);
    statisticsResource.setFeaturestore(featurestore);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(
        featureView, version);
    statisticsResource.setTrainingDataset(trainingDataset);
    return statisticsResource;
  }

  @Path("/version/{version: [0-9]+}/tags")
  public TrainingDatasetTagResource tags(
      @ApiParam(value = "Version of the training dataset", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    tagResource.setProject(project);
    tagResource.setFeatureStore(featurestore);
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(
        featureView, version);
    tagResource.setTrainingDataset(trainingDataset);
    return tagResource;
  }

  @POST
  @Path("/version/{version: [0-9]+}/compute")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Setup a job to compute and write a training dataset", response = JobDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW, ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response compute(@Context UriInfo uriInfo,
      @Context HttpServletRequest req,
      @Context SecurityContext sc,
      @PathParam("version") Integer trainingDatasetVersion,
      TrainingDatasetJobConf trainingDatasetJobConf)
      throws FeaturestoreException, ServiceException, JobException, ProjectException, GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);

    Map<String, String> writeOptions = null;
    if (trainingDatasetJobConf.getWriteOptions() != null) {
      writeOptions = trainingDatasetJobConf.getWriteOptions()
          .stream().collect(Collectors.toMap(OptionDTO::getName, OptionDTO::getValue));
    }

    Jobs job = fsJobManagerController.setupTrainingDatasetJob(project, user, featureView, trainingDatasetVersion,
        trainingDatasetJobConf.getOverwrite(), writeOptions, trainingDatasetJobConf.getSparkJobConfiguration());
    JobDTO jobDTO = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);

    return Response.created(jobDTO.getHref()).entity(jobDTO).build();
  }

  public void setFeatureView(String name, Integer version) throws FeaturestoreException {
    featureView = featureViewController.getByNameVersionAndFeatureStore(name, version, featurestore);
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }
  
  @Path("/version/{version: [0-9]+}/provenance")
  public TrainingDatasetProvenanceResource provenance(@ApiParam(value = "Id of the training dataset")
                                                      @PathParam("version") Integer trainingDatasetVersion) {
    this.provenanceResource.setProject(project);
    this.provenanceResource.setFeatureStore(featurestore);
    this.provenanceResource.setFeatureView(featureView);
    this.provenanceResource.setTrainingDatasetVersion(trainingDatasetVersion);
    return this.provenanceResource;
  }
}
