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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.api.python.environment.command.EnvironmentCommandsResource;
import io.hops.hopsworks.api.python.library.LibraryResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.UnsupportedEncodingException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
import java.util.logging.Level;

@Api(value = "Python Environments Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentResource {

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
  @EJB
  private EnvironmentBuilder environmentBuilder;
  @EJB
  private Settings settings;
  
  private Project project;
  
  public EnvironmentResource setProject(Project project) {
    this.project = project;
    return this;
  }
  
  public Project getProject() {
    return project;
  }
  
  private ResourceRequest getResourceRequest(EnvironmentExpansionBeanParam expansions) throws PythonException {
    if (!project.getConda()) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ENVIRONMENTS);
    if (expansions != null && expansions.getExpansions() != null) {
      resourceRequest.setExpansions(expansions.getResources());
    }
    return resourceRequest;
  }
  
  private EnvironmentDTO buildEnvDTO(UriInfo uriInfo, EnvironmentExpansionBeanParam expansions, String version)
    throws PythonException {
    ResourceRequest resourceRequest = getResourceRequest(expansions);
    return environmentBuilder.build(uriInfo, resourceRequest, project, version);
  }

  @ApiOperation(value = "Get all python environments for this project", response = EnvironmentDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(@BeanParam EnvironmentExpansionBeanParam expansions, @Context UriInfo uriInfo,
    @Context SecurityContext sc) throws PythonException {
    ResourceRequest resourceRequest = getResourceRequest(expansions);
    EnvironmentDTO dto = environmentBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get the python environment for specific python version", response = EnvironmentDTO.class)
  @GET
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@PathParam("version") String version, @BeanParam EnvironmentExpansionBeanParam expansions,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws PythonException {
    if (!version.equals(this.project.getPythonVersion())) {
      throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_ENVIRONMENT_NOT_FOUND, Level.FINE);
    } 
    EnvironmentDTO dto = buildEnvDTO(uriInfo, expansions, version);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Create an environment from version or export an environment as yaml file",
      response = EnvironmentDTO.class)
  @POST
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response post(@PathParam("version") String version,
      @QueryParam("action") EnvironmentDTO.Operation action,
      @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws PythonException, ServiceException, ProjectException {
    EnvironmentDTO dto;
    Users user = jWTHelper.getUserPrincipal(sc);
    switch ((action != null) ? action : EnvironmentDTO.Operation.CREATE) {
      case EXPORT:
        environmentController.exportEnv(project, user, Settings.PROJECT_STAGING_DIR);
        dto = buildEnvDTO(uriInfo, null, version);
        return Response.ok().entity(dto).build();
      case CREATE:
        environmentController.createEnv(project, user, version);
        dto = buildEnvDTO(uriInfo,null, version);
        return Response.created(dto.getHref()).entity(dto).build();
      default:
        throw new WebApplicationException(RESTCodes.ServiceErrorCode.OPERATION_NOT_SUPPORTED.getMessage(),
            Response.Status.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Create an environment from yaml file", response = EnvironmentDTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response postYml(EnvironmentYmlDTO environmentYmlDTO, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws PythonException, ServiceException, DatasetException,
      ProjectException, UnsupportedEncodingException, ElasticException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String allYmlPath = getYmlPath(environmentYmlDTO.getAllYmlPath());
    String cpuYmlPath = getYmlPath(environmentYmlDTO.getCpuYmlPath());
    String gpuYmlPath = getYmlPath(environmentYmlDTO.getGpuYmlPath());
    String version = environmentController.createEnvironmentFromYml(allYmlPath, cpuYmlPath, gpuYmlPath,
      environmentYmlDTO.getInstallJupyter(), user, project);
    EnvironmentDTO dto = buildEnvDTO(uriInfo, null, version);
    return Response.created(dto.getHref()).entity(dto).build();
  }

  @ApiOperation(value = "Remove the python environment with the specified version" + " for this project",
      response = EnvironmentDTO.class)
  @DELETE
  @Path("{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@PathParam("version") String version, @Context SecurityContext sc)
      throws ServiceException, ElasticException {
    Users user = jWTHelper.getUserPrincipal(sc);
    environmentController.removeEnvironment(project, user);
    return Response.noContent().build();
  }

  private String getYmlPath(String path) throws DatasetException, ProjectException, UnsupportedEncodingException {
    if(Strings.isNullOrEmpty(path)) {
      return null;
    }
    DsPath ymlPath = pathValidator.validatePath(this.project, path);
    ymlPath.validatePathExists(inodeController, false);
    org.apache.hadoop.fs.Path fullPath = ymlPath.getFullPath();
    return fullPath.toString();
  }
  
  @ApiOperation(value = "Python library sub-resource", tags = {"PythonLibraryResource"})
  @Path("{version}/libraries")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public LibraryResource libraries(@PathParam("version") String version) {
    return this.libraryResource.setProjectAndVersion(project, version);
  }
  
  @ApiOperation(value = "Python opStatus sub-resource", tags = {"EnvironmentCommandsResource"})
  @Path("{version}/commands")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public EnvironmentCommandsResource opStatus(@PathParam("version") String version) {
    return this.environmentCommandsResource.setProject(project, version);
  }
  
}
