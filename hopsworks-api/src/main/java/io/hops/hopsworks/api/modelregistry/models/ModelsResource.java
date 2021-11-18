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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private Project project;

  private Project modelRegistryProject;

  public ModelsResource setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Sets the model registry of the featuregroups (provided by parent resource)
   *
   * @param modelRegistryId id of the model registry
   * @throws ModelRegistryException
   */
  public void setModelRegistryId(Integer modelRegistryId) throws ModelRegistryException {
    //This call verifies that the project have access to the modelRegistryId provided
    this.modelRegistryProject = modelsController.verifyModelRegistryAccess(project, modelRegistryId).getParentProject();
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
    throws ModelRegistryException, GenericException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(modelsBeanParam.getFilter());
    resourceRequest.setSort(modelsBeanParam.getSortBySet());
    ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, project, modelRegistryProject);
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
    throws ProvenanceException, ModelRegistryException, DatasetException, GenericException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
    resourceRequest.setExpansions(modelsBeanParam.getExpansions().getResources());
    ProvStateDTO fileState = modelsController.getModel(modelRegistryProject, id);
    if(fileState != null) {
      ModelDTO dto = modelsBuilder.build(uriInfo, resourceRequest, project, modelRegistryProject, fileState);
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
    Users hopsworksUser = jwtHelper.getUserPrincipal(sc);
    ProvStateDTO fileState = modelsController.getModel(project, id);
    if(fileState != null) {
      modelsController.delete(hopsworksUser, project, modelRegistryProject, fileState);
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
    validateModelName(modelDTO);
    Users user = jwtHelper.getUserPrincipal(sc);
    Project modelProject = getModelsProjectAndCheckAccess(modelDTO);
    Project experimentProject = getExperimentProjectAndCheckAccess(modelDTO);
    ModelsController.Accessor accessor = getModelsAccessor(user, project, modelProject, experimentProject);
    try {
      return createModel(uriInfo, accessor, id, modelDTO, jobName, kernelId);
    } finally {
      dfs.closeDfsClient(accessor.udfso);
    }
  }

  private void validateModelName(ModelDTO modelDTO) {
    if(!modelDTO.getName().matches("[a-zA-Z0-9_]+")) {
      throw new IllegalArgumentException("Model name must conform to regex: [a-zA-Z0-9_]+");
    }
  }

  private Project getModelsProjectAndCheckAccess(ModelDTO modelDTO)
    throws ProjectException, GenericException, DatasetException {
    Project modelProject;
    if(modelDTO.getProjectName() == null) {
      modelProject = project;
    } else {
      modelProject = projectFacade.findByName(modelDTO.getProjectName());
      if(modelProject == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.INFO,
          "model project not found");
      }
    }
    Dataset modelDataset = datasetCtrl.getByName(modelProject, Settings.HOPS_MODELS_DATASET);
    if(!accessCtrl.hasAccess(project, modelDataset)) {
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO, "models endpoint");
    }
    return modelProject;
  }
  
  private Project getExperimentProjectAndCheckAccess(ModelDTO modelDTO) throws ProjectException, GenericException {
    Project experimentProject;
    if (modelDTO.getExperimentProjectName() == null) {
      experimentProject = project;
    } else {
      experimentProject = projectFacade.findByName(modelDTO.getExperimentProjectName());
      if (experimentProject == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.INFO,
          "experiment project not found for model");
      }
    }
    if (!experimentProject.getId().equals(project.getId())) {
      String usrMsg = "writing to shared experiment is not yet allowed";
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO, usrMsg, usrMsg);
    }
    return experimentProject;
  }
  
  private ModelsController.Accessor getModelsAccessor(Users user, Project userProject, Project modelProject,
    Project experimentProject)
    throws DatasetException {
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUser = hdfsUsersController.getHdfsUserName(experimentProject, user);
      udfso = dfs.getDfsOps(hdfsUser);
      return new ModelsController.Accessor(user, userProject, modelProject, experimentProject, udfso, hdfsUser);
    } catch (Throwable t) {
      if(udfso != null){
        dfs.closeDfsClient(udfso);
      }
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.INFO);
    }
  }
  
  private Response createModel(UriInfo uriInfo, ModelsController.Accessor accessor, String mlId, ModelDTO modelDTO,
                               String jobName, String kernelId)
          throws DatasetException, MetadataException, JobException, ServiceException, PythonException,
          ModelRegistryException {
    String realName = accessor.user.getFname() + " " + accessor.user.getLname();

    //Only attach program and environment if exporting inside Hopsworks
    if(!Strings.isNullOrEmpty(jobName) || !Strings.isNullOrEmpty(kernelId)) {

      modelDTO.setProgram(modelsController.versionProgram(accessor, jobName, kernelId,
              modelDTO.getName(), modelDTO.getVersion()));

      //Export environment to correct path here
      modelDTO.setEnvironment(environmentController.exportEnv(accessor.experimentProject, accessor.user,
              Utils.getProjectPath(accessor.modelProject.getName()) +
                      Settings.HOPS_MODELS_DATASET + "/" + modelDTO.getName() + "/" + modelDTO.getVersion() +
                      "/" + Settings.ENVIRONMENT_FILE
              ));
    }

    modelDTO.setModelRegistryId(accessor.modelProject.getId());

    modelsController.attachModel(accessor.udfso, accessor.modelProject, realName, modelDTO);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(mlId);
    return Response.created(builder.build()).entity(modelDTO).build();
  }
}
