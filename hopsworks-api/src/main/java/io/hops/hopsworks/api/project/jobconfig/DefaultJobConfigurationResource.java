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

package io.hops.hopsworks.api.project.jobconfig;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DefaultJobConfigurationResource {

  private static final Logger LOGGER = Logger.getLogger(DefaultJobConfigurationResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private ProjectController projectController;

  @EJB
  private DefaultJobConfigurationBuilder defaultJobConfigurationBuilder;

  private Project project;
  @Logged(logLevel = LogLevel.OFF)
  public DefaultJobConfigurationResource setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }

  @ApiOperation(value = "Get all the default job configurations", response = DefaultJobConfigurationDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam DefaultJobConfigurationBeanParam defaultJobConfigurationBeanParam,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.JOBCONFIG);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(defaultJobConfigurationBeanParam.getSortBySet());
    resourceRequest.setFilter(defaultJobConfigurationBeanParam.getFilter());
    DefaultJobConfigurationDTO defaultJobConfigurationDTO =
      this.defaultJobConfigurationBuilder.build(uriInfo, resourceRequest, this.project);
    if(defaultJobConfigurationDTO == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_DEFAULT_JOB_CONFIG_NOT_FOUND, Level.FINEST);
    }
    return Response.ok(defaultJobConfigurationDTO).build();
  }

  @ApiOperation(value = "Get the default job configuration by JobType", response = DefaultJobConfigurationDTO.class)
  @GET
  @Path("{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByType(@PathParam("type") JobType jobType,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.JOBCONFIG);
    DefaultJobConfiguration defaultJobConfiguration =
      projectController.getProjectDefaultJobConfiguration(project, jobType);
    if(defaultJobConfiguration == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_DEFAULT_JOB_CONFIG_NOT_FOUND, Level.FINEST);
    } else {
      DefaultJobConfigurationDTO defaultJobConfigurationDTO =
        this.defaultJobConfigurationBuilder.build(uriInfo, resourceRequest, defaultJobConfiguration, jobType);
      return Response.ok(defaultJobConfigurationDTO).build();
    }
  }

  @ApiOperation(value = "Create or update the default job configuration", response = DefaultJobConfigurationDTO.class)
  @PUT
  @Path("{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response put (
    @ApiParam(value = "Job configuration", required = true) JobConfiguration config,
    @PathParam("type") JobType type,
    @Context UriInfo uriInfo,
    @Context HttpServletRequest req,
    @Context SecurityContext sc) throws ProjectException {

    Response.Status status = Response.Status.CREATED;

    HopsUtils.validateJobConfigurationType(config, type);

    DefaultJobConfiguration currentConfig = projectController.getProjectDefaultJobConfiguration(project, type);
    if(currentConfig != null) {
      status = Response.Status.OK;
    }

    DefaultJobConfiguration defaultConfig =
      projectController.createOrUpdateDefaultJobConfig(this.project, config, type, currentConfig);

    DefaultJobConfigurationDTO defaultJobConfigurationDTO =
      this.defaultJobConfigurationBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBCONFIG),
        defaultConfig, type);

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();

    if(status == Response.Status.CREATED) {
      return Response.created(builder.build()).entity(defaultJobConfigurationDTO).build();
    } else {
      return Response.ok(builder.build()).entity(defaultJobConfigurationDTO).build();
    }
  }

  @ApiOperation(value = "Delete the default job configuration")
  @DELETE
  @Path("{type}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@PathParam("type") JobType type,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) {
    projectController.removeProjectDefaultJobConfiguration(this.project, type);
    return Response.noContent().build();
  }
}
