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
import io.hops.hopsworks.api.python.command.CommandBeanParam;
import io.hops.hopsworks.api.python.command.CommandBuilder;
import io.hops.hopsworks.api.python.command.CommandDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam CommandBeanParam environmentsCommandBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion);
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByName(@PathParam("commandId") Integer commandId, @Context UriInfo uriInfo,
    @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMANDS);
    CommandDTO dto = commandBuilder.build(uriInfo, resourceRequest, project, commandId);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Delete commands for this environment")
  @DELETE
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion);
    commandsController.deleteCommands(project);
    return Response.noContent().build();
  }
}
