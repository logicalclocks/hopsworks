/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.python.environment.command.custom;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.python.command.CommandBuilder;
import io.hops.hopsworks.api.python.command.CommandDTO;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.python.commands.custom.CustomCommandsController;
import io.hops.hopsworks.common.python.commands.custom.CustomCommandsSettings;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

@Logged
@Api(value = "Python Environment Custom Commands Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentCustomCommandsResource {
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private CustomCommandsController customCommandsController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private CommandBuilder commandBuilder;

  private Project project;
  private String pythonVersion;

  @Logged(logLevel = LogLevel.OFF)
  public EnvironmentCustomCommandsResource setProject(Project project, String pythonVersion) {
    this.project = project;
    this.pythonVersion = pythonVersion;
    return this;
  }

  @ApiOperation(value = "Build the environment with custom bash commands")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response build(CustomCommandsSettings commandsSettings, @Context UriInfo uriInfo,
                        @Context HttpServletRequest req,
                        @Context SecurityContext sc)
      throws PythonException, IOException, DatasetException, ServiceException {
    environmentController.checkCondaEnabled(project, pythonVersion, false);
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMANDS);
    environmentController.checkCondaEnvExists(project, user);
    CondaCommands cc = customCommandsController.buildEnvWithCustomCommands(project, user, commandsSettings,
        pythonVersion);
    CommandDTO dto = commandBuilder.build(uriInfo, resourceRequest, cc);
    return Response.ok().entity(dto).build();
  }
}
