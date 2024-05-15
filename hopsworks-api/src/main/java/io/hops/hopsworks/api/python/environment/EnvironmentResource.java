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
package io.hops.hopsworks.api.python.environment;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.ProjectSubResource;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.api.python.conflicts.EnvironmentConflictsResource;
import io.hops.hopsworks.api.python.environment.command.EnvironmentCommandsResource;
import io.hops.hopsworks.api.python.environment.history.EnvironmentHistoryResource;
import io.hops.hopsworks.api.python.library.LibraryResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

@Api(value = "Python Environments Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentResource extends ProjectSubResource {

  @EJB
  private EnvironmentController environmentController;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private InodeController inodeController;
  @EJB
  private JWTHelper jWTHelper;
  @Inject
  private LibraryResource libraryResource;
  @Inject
  private EnvironmentCommandsResource environmentCommandsResource;
  @Inject
  private EnvironmentConflictsResource environmentConflictsResource;
  @Inject
  private EnvironmentHistoryResource environmentHistoryResource;
  @EJB
  private EnvironmentBuilder environmentBuilder;
  @EJB
  private ProjectController projectController;
  
  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }
  
  private ResourceRequest getResourceRequest(EnvironmentExpansionBeanParam expansions, Project project)
      throws PythonException {
    if (project.getPythonEnvironment() == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ENVIRONMENTS);
    if (expansions != null && expansions.getExpansions() != null) {
      resourceRequest.setExpansions(expansions.getResources());
    }
    return resourceRequest;
  }
  
  private EnvironmentDTO buildEnvDTO(UriInfo uriInfo, EnvironmentExpansionBeanParam expansions, Project project,
      String version) throws PythonException, IOException, ServiceDiscoveryException{
    ResourceRequest resourceRequest = getResourceRequest(expansions, project);
    return environmentBuilder.build(uriInfo, resourceRequest, project, version);
  }

  @ApiOperation(value = "Get all python environments for this project", response = EnvironmentDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAll(@BeanParam EnvironmentExpansionBeanParam expansions,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws PythonException, IOException, ServiceDiscoveryException, ProjectException {
    Project project = getProject();
    ResourceRequest resourceRequest = getResourceRequest(expansions, project);
    EnvironmentDTO dto = environmentBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get the python environment for specific python version", response = EnvironmentDTO.class)
  @GET
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@PathParam("version") String version,
                      @BeanParam EnvironmentExpansionBeanParam expansions,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws PythonException, IOException,
      ServiceDiscoveryException, ProjectException {
    Project project = getProject();
    if (project.getPythonEnvironment() == null ||
        !project.getPythonEnvironment().getPythonVersion().equals(version)) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    } 
    EnvironmentDTO dto = buildEnvDTO(uriInfo, expansions, project, version);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Create an environment from version or export an environment as yaml file",
      response = EnvironmentDTO.class)
  @POST
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response post(@PathParam("version") String version,
                       @QueryParam("action") EnvironmentDTO.Operation action,
                       @Context UriInfo uriInfo,
                       @Context HttpServletRequest req,
                       @Context SecurityContext sc)
      throws PythonException, ServiceException, IOException, ServiceDiscoveryException, ProjectException {
    EnvironmentDTO dto;
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    switch ((action != null) ? action : EnvironmentDTO.Operation.CREATE) {
      case EXPORT:
        environmentController.exportEnv(project, user, environmentController.getDockerImageEnvironmentFile(project));
        dto = buildEnvDTO(uriInfo, null, project, version);
        return Response.ok().entity(dto).build();
      case CREATE:
        environmentController.createEnv(project, user);
        dto = buildEnvDTO(uriInfo,null, project, version);
        return Response.created(dto.getHref()).entity(dto).build();
      default:
        throw new WebApplicationException(RESTCodes.ServiceErrorCode.OPERATION_NOT_SUPPORTED.getMessage(),
            Response.Status.NOT_FOUND);
    }
  }
  
  @ApiOperation(value = "Create an environment from a import file", response = EnvironmentDTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response postImport(EnvironmentImportDTO environmentImportDTO,
                             @Context UriInfo uriInfo,
                             @Context SecurityContext sc)
      throws PythonException, ServiceException, DatasetException, IOException, ProjectException,
      ServiceDiscoveryException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    String version = environmentController.createProjectDockerImageFromImport(
        getYmlPath(environmentImportDTO.getPath(), project),
        environmentImportDTO.getInstallJupyter(), user, project);
    EnvironmentDTO dto = buildEnvDTO(uriInfo,null, project, version);
    return Response.created(dto.getHref()).entity(dto).build();
  }

  @ApiOperation(value = "Remove the python environment with the specified version for this project",
      response = EnvironmentDTO.class)
  @DELETE
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response delete(@PathParam("version") String version,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) throws PythonException, ProjectException {
    environmentController.removeEnvironment(getProject());
    return Response.noContent().build();
  }

  private String getYmlPath(String path, Project project) throws DatasetException, ProjectException,
      UnsupportedEncodingException {
    if(Strings.isNullOrEmpty(path)) {
      return null;
    }
    DsPath ymlPath = pathValidator.validatePath(project, path);
    ymlPath.validatePathExists(inodeController, false);
    org.apache.hadoop.fs.Path fullPath = ymlPath.getFullPath();
    return fullPath.toString();
  }
  
  @ApiOperation(value = "Python library sub-resource", tags = {"PythonLibraryResource"})
  @Path("{version}/libraries")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public LibraryResource libraries(@PathParam("version") String version) {
    this.libraryResource.setProjectId(getProjectId());
    this.libraryResource.setPythonVersion(version);
    return this.libraryResource;
  }
  
  @ApiOperation(value = "Python opStatus sub-resource", tags = {"EnvironmentCommandsResource"})
  @Path("{version}/commands")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public EnvironmentCommandsResource commands(@PathParam("version") String version) {
    this.environmentCommandsResource.setProjectId(getProjectId());
    this.environmentCommandsResource.setPythonVersion(version);
    return this.environmentCommandsResource;
  }

  @ApiOperation(value = "Python conflicts sub-resource", tags = {"EnvironmentConflictsResource"})
  @Path("{version}/conflicts")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public EnvironmentConflictsResource conflicts(@PathParam("version") String version) {
    this.environmentConflictsResource.setProjectId(getProjectId());
    this.environmentConflictsResource.setPythonVersion(version);
    return this.environmentConflictsResource;
  }

  @ApiOperation(value = "Environment history subresource", tags = {"EnvironmentHistoryResource"})
  @Path("{version}/history")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public EnvironmentHistoryResource environmentHistory(@PathParam("version") String version) {
    this.environmentHistoryResource.setProjectId(getProjectId());
    this.environmentHistoryResource.setPythonVersion(version);
    return this.environmentHistoryResource;
  }
}
