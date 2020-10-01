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

package io.hops.hopsworks.api.jobs.executions;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExecutionsResource {
  
  @Inject
  private ExecutionController executionController;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  
  
  @EJB
  private JWTHelper jWTHelper;
  
  private Jobs job;
  
  public ExecutionsResource setJob(Jobs job) {
    this.job = job;
    return this;
  }
  
  @ApiOperation(value = "Get a list of executions for the job.", response = ExecutionDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecutions(
    @BeanParam Pagination pagination,
    @BeanParam ExecutionsBeanParam executionsBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTIONS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(executionsBeanParam.getSortBySet());
    resourceRequest.setFilter(executionsBeanParam.getFilter());
    resourceRequest.setExpansions(executionsBeanParam.getExpansions().getResources());
    
    ExecutionDTO dto = executionsBuilder.build(uriInfo, resourceRequest, job);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Find Execution by Id", response = ExecutionDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecution(@ApiParam(value = "execution id", required = true) @PathParam("id") Integer id,
    @BeanParam ExecutionsBeanParam executionsBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws JobException {
    //If requested execution does not belong to job
    Execution execution = executionController.authorize(job, id);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTIONS);
    resourceRequest.setExpansions(executionsBeanParam.getExpansions().getResources());
    ExecutionDTO dto = executionsBuilder.build(uriInfo, resourceRequest, execution);
    return Response.ok().entity(dto).build();
  }
  
  
  @ApiOperation(value = "Stop an execution(run) of the job",
    notes = "Stops an execution of a job by providing the status.",
    response = ExecutionDTO.class)
  @PUT
  @Path("{id}/status")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopExecution(
    @ApiParam(value = "Id of execution.", required = true) @PathParam("id") Integer id,
    @ApiParam(value = "status to set.", required = true) Status status,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws JobException {
    
    Execution exec = executionController.stopExecution(id);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTIONS);
    return Response.accepted().entity(executionsBuilder.build(uriInfo, resourceRequest, exec)).build();
  }
  
  @ApiOperation(value = "Start an execution(run) of the job",
    notes = "Starts a job by creating and starting an Execution, stops a job by stopping the Execution.",
    response = ExecutionDTO.class)
  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startExecution(
    @ApiParam(value = "Arguments for executing the job") String args,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws JobException, GenericException, ServiceException, ProjectException {
    
    Users user = jWTHelper.getUserPrincipal(sc);

    Execution exec;
    if(!Strings.isNullOrEmpty(job.getJobConfig().getDefaultArgs()) && Strings.isNullOrEmpty(args)) {
      exec = executionController.start(job, job.getJobConfig().getDefaultArgs(), user);
    } else {
      exec = executionController.start(job, args, user);
    }

    UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
    uriBuilder.path(Integer.toString(exec.getId()));
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTIONS);
    return Response.created(uriBuilder.build()).entity(executionsBuilder.build(uriInfo, resourceRequest, exec))
      .build();
  }
  
  @ApiOperation(value = "Delete an execution of a job by Id", response = ExecutionDTO.class)
  @DELETE
  @Path("{id}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@ApiParam(value = "execution id", required = true) @PathParam("id") Integer id,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws JobException {
    //If requested execution does not belong to job
    Execution execution = executionController.authorize(job, id);
    executionController.delete(execution);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Retrieve log of given execution and type", response = JobLogDTO.class)
  @GET
  @Path("{id}/log/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getLog(
    @PathParam("id") Integer id,
    @PathParam("type") JobLogDTO.LogType type, @Context SecurityContext sc) throws JobException {
    Execution execution = executionController.authorize(job, id);
    JobLogDTO dto = executionController.getLog(execution, type);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Retry log aggregation of given execution and type", response = JobLogDTO.class)
  @POST
  @Path("{id}/log/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response retryLog(
    @PathParam("id") Integer id,
    @PathParam("type") JobLogDTO.LogType type, @Context SecurityContext sc) throws JobException {
    Execution execution = executionController.authorize(job, id);
    JobLogDTO dto = executionController.retryLogAggregation(execution, type);
    return Response.ok().entity(dto).build();
  }
  
  public enum Status {
    STOPPED("stopped");
    
    private final String name;
    
    Status(String name) {
      this.name = name;
    }
    
    public static Status fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    public String getName() {
      return name;
    }
    
    @Override
    public String toString() {
      return name;
    }
  }
  
}