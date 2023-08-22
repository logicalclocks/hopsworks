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

package io.hops.hopsworks.api.jobs.scheduler;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2Controller;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Job Schedule resource")
public class JobScheduleV2Resource {

  private Jobs job;
  @EJB
  private JobScheduleV2Controller jobScheduleController;
  @EJB
  private JobScheduleV2Builder jobScheduleBuilder;

  @ApiOperation(value = "Get job's schedule.", response = JobScheduleV2DTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSchedule(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo) throws JobException {
    JobScheduleV2 jobSchedule = jobScheduleController.getScheduleByJobId(job.getId());
    JobScheduleV2DTO scheduleDTO = jobScheduleBuilder.build(uriInfo, jobSchedule);
    return Response.ok().entity(scheduleDTO).build();
  }

  @ApiOperation(value = "Create job's schedule.", response = JobScheduleV2DTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createSchedule(JobScheduleV2DTO scheduleDTO,
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo) {
    JobScheduleV2 jobSchedule = jobScheduleController.createSchedule(
        jobScheduleBuilder.validateAndConvertOnCreate(job, scheduleDTO)
    );
    scheduleDTO = jobScheduleBuilder.build(uriInfo, jobSchedule);
    return Response.created(scheduleDTO.getHref()).entity(scheduleDTO).build();
  }

  @ApiOperation(value = "Update job's schedule.", response = JobScheduleV2DTO.class)
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateSchedule(JobScheduleV2DTO scheduleDTO,
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo) throws JobException {
    jobScheduleBuilder.validateOnUpdate(job, scheduleDTO);
    JobScheduleV2 jobSchedule = jobScheduleController.updateSchedule(scheduleDTO);
    scheduleDTO = jobScheduleBuilder.build(uriInfo, jobSchedule);
    return Response.ok().entity(scheduleDTO).build();
  }

  /**
   * Remove scheduling for the job with this jobid.
   */
  @ApiOperation(value = "Delete a job's schedule.")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB},
          allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteSchedule(
      @Context HttpServletRequest req,
      @Context SecurityContext sc) {
    jobScheduleController.deleteSchedule(job.getId());
    return Response.noContent().build();
  }

  public JobScheduleV2Resource setJob(Jobs job) {
    this.job = job;
    return this;
  }

}
