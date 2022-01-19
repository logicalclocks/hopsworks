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
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
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

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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
import javax.ws.rs.core.UriInfo;
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

  private Project userProject;

  private Project modelRegistryProject;

  @Logged(logLevel = LogLevel.OFF)
  public ModelsResource setProject(Project project) {
    this.userProject = project;
    return this;
  }

  /**
   * Sets the model registry of the featuregroups (provided by parent resource)
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
  public Response put (
    @PathParam("id") String id,
    ModelDTO modelDTO,
    @QueryParam("jobName") String jobName,
    @QueryParam("kernelId") String kernelId,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc)
    throws DatasetException, ModelRegistryException, JobException, ServiceException, PythonException, MetadataException,
    GenericException, ProjectException {
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
}