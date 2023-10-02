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
package io.hops.hopsworks.api.python.environment.command;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.python.command.CommandBeanParam;
import io.hops.hopsworks.api.python.command.CommandBuilder;
import io.hops.hopsworks.api.python.command.CommandDTO;
import io.hops.hopsworks.api.python.environment.command.custom.EnvironmentCustomCommandsResource;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Api(value = "Python Environment Commands Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentCommandsResource {

  @EJB
  private CommandsController commandsController;
  @EJB
  private CommandBuilder commandBuilder;
  @EJB
  private EnvironmentController environmentController;
  @Inject
  private EnvironmentCustomCommandsResource environmentCustomCommandsResource;
  
  private Project project;
  private String pythonVersion;
  public EnvironmentCommandsResource setProject(Project project, String pythonVersion) {
    this.project = project;
    this.pythonVersion = pythonVersion;
    return this;
  }
  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Get commands for this environment")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam CommandBeanParam environmentsCommandBeanParam,
                      @Context UriInfo uriInfo,
                      @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMANDS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(environmentsCommandBeanParam.getSortBySet());
    resourceRequest.setFilter(environmentsCommandBeanParam.getFilter());
    CommandDTO dto = commandBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get commands by id", response = CommandDTO.class)
  @GET
  @Path("{commandId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByName(@PathParam("commandId") Integer commandId,
                            @Context UriInfo uriInfo,
                            @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMANDS);
    CommandDTO dto = commandBuilder.build(uriInfo, resourceRequest, project, commandId);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Delete commands for this environment")
  @DELETE
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteAll(@Context SecurityContext sc,
                            @Context HttpServletRequest req) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    commandsController.deleteCommands(project);
    return Response.noContent().build();
  }

  @ApiOperation(value = "Delete a command for this environment")
  @DELETE
  @Path("{commandId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteCommand(@Context SecurityContext sc, @PathParam("commandId") Integer commandId,
                            @Context HttpServletRequest req) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    commandsController.deleteCommand(project,commandId);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Update commands for this environment")
  @PUT
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response update(@Context UriInfo uriInfo,
                         @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    commandsController.retryFailedCondaEnvOps(project);
    return Response.noContent().build();
  }

  @ApiOperation(value = "Custom commands sub-resource", tags = {"EnvironmentCustomCommandsResource"})
  @Path("custom")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public EnvironmentCustomCommandsResource commands() {
    return this.environmentCustomCommandsResource.setProject(project, pythonVersion);
  }
}
