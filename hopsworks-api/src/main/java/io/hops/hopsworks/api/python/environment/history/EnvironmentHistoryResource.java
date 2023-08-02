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
package io.hops.hopsworks.api.python.environment.history;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@Api(value = "Python Environments History Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentHistoryResource {

  @EJB
  private EnvironmentHistoryBuilder environmentHistoryBuilder;
  @EJB
  private JWTHelper jwtHelper;

  private Project project;
  private String version;

  @Logged(logLevel = LogLevel.OFF)
  public EnvironmentHistoryResource init(Project project, String version) {
    this.project = project;
    this.version = version;
    return this;
  }

  @Logged(logLevel = LogLevel.OFF)
  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Get the environment history for this project", response = EnvironmentHistoryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(@PathParam("version") String version,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc,
                         @BeanParam EnvironmentHistoryBeanParam environmentHistoryBeanParam,
                         @BeanParam Pagination pagination) throws ServiceException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ENVIRONMENT_HISTORY);
    resourceRequest.setExpansions(environmentHistoryBeanParam.getExpansions().getResources());
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(environmentHistoryBeanParam.getFilter());
    resourceRequest.setSort(environmentHistoryBeanParam.getSortBySet());
    EnvironmentHistoryDTO dto = environmentHistoryBuilder.build(uriInfo, resourceRequest, project, version);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get a build in history by a particular Id", response = EnvironmentHistoryDTO.class)
  @GET
  @Path("/{buildId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getEnvironmentDelta(@PathParam("buildId") Integer buildId,
                                @Context SecurityContext sc,
                                @Context HttpServletRequest req,
                                @Context UriInfo uriInfo,
                                @BeanParam EnvironmentHistoryBeanParam environmentHistoryBeanParam)
      throws ServiceException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ENVIRONMENT_HISTORY);
    resourceRequest.setExpansions(environmentHistoryBeanParam.getExpansions().getResources());
    EnvironmentHistoryDTO dto = environmentHistoryBuilder.build(uriInfo, resourceRequest, project, buildId);
    return Response.ok().entity(dto).build();
  }
}
