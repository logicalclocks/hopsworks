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
package io.hops.hopsworks.api.modelregistry.models;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.modelregistry.models.tags.ModelTagsBuilder;
import io.hops.hopsworks.api.tags.TagsDTO;
import io.hops.hopsworks.api.tags.TagsExpansionBeanParam;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.models.tags.ModelTagControllerIface;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsResource {

  private static final Logger LOGGER = Logger.getLogger(ModelsResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ModelsBuilder modelsBuilder;
  @EJB
  private ModelsController modelsController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private ModelUtils modelUtils;
  @Inject
  private ModelTagControllerIface tagController;
  @EJB
  private ModelTagsBuilder tagBuilder;

  private Project userProject;

  private Project modelRegistryProject;

  @Logged(logLevel = LogLevel.OFF)
  public ModelsResource setProject(Project project) {
    this.userProject = project;
    return this;
  }

  /**
   * Sets the model registry of the models
   *
   * @param modelRegistryId id of the model registry
   * @throws ModelRegistryException
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setModelRegistryId(Integer modelRegistryId) throws ModelRegistryException {
    //This call verifies that the project have access to the modelRegistryId provided
    this.modelRegistryProject = modelsController.verifyModelRegistryAccess(userProject,
        modelRegistryId).getParentProject();
  }

  @ApiOperation(value = "Get a list of all models for this project", response = ModelDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam ModelsBeanParam modelsBeanParam,
    @Context UriInfo uriInfo,
    @Context HttpServletRequest req,
    @Context SecurityContext sc)
          throws ModelRegistryException, GenericException, SchematizedTagException, MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(modelsBeanParam.getFilter());
    resourceRequest.setExpansions(modelsBeanParam.getExpansions().getResources());
    resourceRequest.setSort(modelsBeanParam.getSortBySet());
    ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, user, userProject, modelRegistryProject);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation( value = "Get a model", response = ModelDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get (
    @PathParam("id") String id,
    @BeanParam ModelsBeanParam modelsBeanParam,
    @Context UriInfo uriInfo,
    @Context HttpServletRequest req,
    @Context SecurityContext sc)
          throws ProvenanceException, ModelRegistryException, DatasetException, GenericException,
          SchematizedTagException, MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    resourceRequest.setExpansions(modelsBeanParam.getExpansions().getResources());
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);

    if(fileState != null) {
      ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, user, userProject, modelRegistryProject, fileState,
              modelUtils.getModelsDatasetPath(userProject, modelRegistryProject));
      if(dto == null) {
        throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.FINE);
      } else {
        return Response.ok().entity(dto).build();
      }
    } else {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_NOT_FOUND, Level.FINE);
    }
  }

  @ApiOperation( value = "Delete a model")
  @DELETE
  @Path("{id}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete (
    @PathParam("id") String id,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc) throws DatasetException, ProvenanceException, ModelRegistryException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(userProject, id);
    if(fileState != null) {
      modelsController.delete(user, userProject, modelRegistryProject, fileState);
    }
    return Response.noContent().build();
  }

  @ApiOperation( value = "Create or update a model", response = ModelDTO.class)
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response put(@PathParam("id") String id,
                      ModelDTO modelDTO,
                      @QueryParam("jobName") String jobName,
                      @QueryParam("kernelId") String kernelId,
                      @Context HttpServletRequest req,
                      @Context UriInfo uriInfo,
                      @Context SecurityContext sc)
      throws DatasetException, ModelRegistryException, JobException, ServiceException, PythonException,
      MetadataException, GenericException, ProjectException {
    if (modelDTO == null) {
      throw new IllegalArgumentException("Model summary not provided");
    }
    modelUtils.validateModelName(modelDTO);
    Users user = jwtHelper.getUserPrincipal(sc);
    Project modelProject = modelUtils.getModelsProjectAndCheckAccess(modelDTO, userProject);
    Project experimentProject = modelUtils.getExperimentProjectAndCheckAccess(modelDTO, userProject);
    ModelsController.Accessor accessor = modelUtils.getModelsAccessor(user, userProject, modelProject,
        experimentProject);
    try {
      return modelUtils.createModel(uriInfo, accessor, id, modelDTO, jobName, kernelId);
    } finally {
      dfs.closeDfsClient(accessor.udfso);
    }
  }

  @ApiOperation( value = "Create or update one tag for a model", response = TagsDTO.class)
  @PUT
  @Path("/{id}/tags/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(@Context SecurityContext sc,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the model", required = true)
                         @PathParam("id") String id,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @ApiParam(value = "Value to set for the tag") String value)
      throws MetadataException, SchematizedTagException, DatasetException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);

    AttachTagResult result = tagController.upsert(userProject, user,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()), name, value);

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, userProject, modelRegistryProject,
        fileState.getMlId(), result.getItems());

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }

  @ApiOperation( value = "Create or update tags(bulk) for a model", response = TagsDTO.class)
  @PUT
  @Path("/{id}/tags")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response bulkPutTags(@Context SecurityContext sc,
                              @Context HttpServletRequest req,
                              @Context UriInfo uriInfo,
                              @ApiParam(value = "Id of the model", required = true)
                              @PathParam("id") String id,
                              TagsDTO tags)
      throws MetadataException, SchematizedTagException, DatasetException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);

    AttachTagResult result;

    if(tags.getItems().size() == 0) {
      result = tagController.upsert(userProject, user,
          modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()),
          tags.getName(), tags.getValue());
    } else {
      Map<String, String> newTags = new HashMap<>();
      for(TagsDTO tag : tags.getItems()) {
        newTags.put(tag.getName(), tag.getValue());
      }
      result = tagController.upsertAll(userProject, user,
          modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()), newTags);
    }

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, userProject, modelRegistryProject,
        fileState.getMlId(), result.getItems());

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }

  @ApiOperation( value = "Get all tags attached to a model", response = TagsDTO.class)
  @GET
  @Path("/{id}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(@Context SecurityContext sc,
                          @Context HttpServletRequest req,
                          @Context UriInfo uriInfo,
                          @ApiParam(value = "Id of the model", required = true)
                          @PathParam("id") String id,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
      throws DatasetException, MetadataException, SchematizedTagException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);
    Map<String, String> result = tagController.getAll(userProject, user,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()));

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, userProject, modelRegistryProject,
        fileState.getMlId(), result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Get tag attached to a model", response = TagsDTO.class)
  @GET
  @Path("/{id}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(@Context SecurityContext sc,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @ApiParam(value = "Id of the model", required = true)
                         @PathParam("id") String id,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
      throws DatasetException, MetadataException, SchematizedTagException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);
    Map<String, String> result = new HashMap<>();
    result.put(name, tagController.get(userProject, user,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()), name));

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagBuilder.build(uriInfo, resourceRequest, userProject, modelRegistryProject,
        fileState.getMlId(), result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }

  @ApiOperation( value = "Delete all tags attached to a model")
  @DELETE
  @Path("/{id}/tags")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @Context HttpServletRequest req,
                             @ApiParam(value = "Id of the model", required = true)
                             @PathParam("id") String id)
      throws DatasetException, MetadataException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);

    tagController.deleteAll(userProject, user,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()));

    return Response.noContent().build();
  }

  @ApiOperation( value = "Delete tag attached to a model")
  @DELETE
  @Path("/{id}/tags/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.MODELREGISTRY}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @Context HttpServletRequest req,
                            @ApiParam(value = "Id of the model", required = true)
                            @PathParam("id") String id,
                            @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name)
      throws DatasetException, MetadataException, ProvenanceException, ModelRegistryException {

    Users user = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    ModelDTO model = modelUtils.convertProvenanceHitToModel(fileState);

    tagController.delete(userProject, user,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()), name);

    return Response.noContent().build();
  }
}
