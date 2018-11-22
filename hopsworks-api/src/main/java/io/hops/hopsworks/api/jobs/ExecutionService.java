/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.jobs.AppInfoDTO;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExecutionService {
  
  private static final Logger LOGGER = Logger.getLogger(ExecutionService.class.getName());
  
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private ExecutionController executionController;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  
  
  @EJB
  private JWTHelper jWTHelper;
  
  private Jobs job;
  
  ExecutionService setJob(Jobs job) {
    this.job = job;
    return this;
  }
  
  @ApiOperation(value = "Get a list of executions for the job.", response = ExecutionDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecutions(
    @BeanParam Pagination pagination,
    @BeanParam ExecutionsBeanParam executionsBeanParam,
    @ApiParam(value = "comma-separated list of entities to expand in the collection")
    @QueryParam("expand") String expand,
    @Context UriInfo uriInfo) {

    ExecutionDTO executionDTO = executionsBuilder.build(uriInfo,
      new ResourceProperties(ResourceProperties.Name.EXECUTIONS, pagination.getOffset(), pagination.getLimit(),
        executionsBeanParam.getSortBySet(), executionsBeanParam.getFilter(), expand), job);

    GenericEntity<ExecutionDTO> entity = new GenericEntity<ExecutionDTO>(
      executionDTO) {
    };
    return Response.ok().entity(entity).build();
  }
  
  @ApiOperation(value = "Find Execution by Id", response = ExecutionDTO.class)
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecution(
    @ApiParam(value = "execution id", required = true) @PathParam("id") Integer id,
    @ApiParam(value = "user") @QueryParam("expand") String expand,
    @Context UriInfo uriInfo) throws JobException {
    //If requested execution does not belong to job
    Execution execution = authorize(id);
    
    ExecutionDTO dto = executionsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.EXECUTIONS,
      expand), execution);
    return Response.ok().entity(dto).build();
  }
  
  
  @ApiOperation(value = "Start/Stop a job",
    notes = "Starts a job by creating and starting an Execution, stops a job by stopping the Execution.")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response execution(
    @ApiParam(value = "start or stop", required = true) @QueryParam("action")
      Action action,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException, GenericException {
    
    Users user = jWTHelper.getUserPrincipal(req);
    Execution exec;
    switch (action) {
      case START:
        exec = executionController.start(job, user);
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.path(Integer.toString(exec.getId()));
        return Response.created(uriBuilder.build()).entity(executionsBuilder.build(uriInfo,
          new ResourceProperties(ResourceProperties.Name.EXECUTIONS), exec)).build();
      case STOP:
        exec = executionController.kill(job, user);
        return Response.ok().entity(executionsBuilder.build(uriInfo,
          new ResourceProperties(ResourceProperties.Name.EXECUTIONS), exec)).build();
      default:
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
    
  }
  
  @ApiOperation(value = "Retrieve log of given execution and type", response = JobLogDTO.class)
  @GET
  @Path("{id}/log/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getLog(
    @PathParam("id") Integer id,
    @PathParam("type") JobLogDTO.LogType type) throws JobException {
    Execution execution = authorize(id);
    JobLogDTO dto = executionController.getLog(execution, type);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Retry log aggregation of given execution and type", response = JobLogDTO.class)
  @POST
  @Path("{id}/log/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response retryLog(
    @PathParam("id") Integer id,
    @PathParam("type") JobLogDTO.LogType type) throws JobException {
    Execution execution = authorize(id);
    JobLogDTO dto = executionController.retryLogAggregation(execution, type);
    return Response.ok().entity(dto).build();
  }
  
  
  @ApiOperation(value = "Get Application UI url, i.e. Spark or Flink, for given Execution by Id")
  @GET
  @Path("{id}/ui")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecutionUI(
    @ApiParam(value = "id", required = true) @PathParam("id") Integer id,
    @Context UriInfo uriInfo) throws JobException {
    Execution execution = authorize(id);
    String url = executionController.getExecutionUI(execution);
    return Response.ok().entity(url).build();
  }
  
  @ApiOperation(value = "Get YARN UI url for given Execution by Id")
  @GET
  @Path("{id}/yarnui")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getYarnUI(
    @ApiParam(value = "id", required = true) @PathParam("id") Integer id,
    @Context UriInfo uriInfo) throws JobException {
    authorize(id);
    String url = executionController.getExecutionYarnUI(id);
    return Response.ok().entity(url).build();
  }
  
  @ApiOperation(value = "Get Application specific information for given Execution")
  @GET
  @Path("{id}/appinfo")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecutionAppInfo(
    @ApiParam(value = "id", required = true) @PathParam("id") Integer id,
    @Context UriInfo uriInfo) throws JobException {
    Execution execution = authorize(id);
    AppInfoDTO dto = executionController.getExecutionAppInfo(execution);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get Application specific information for given Execution.")
  @GET
  @Path("{id}/prox/{path: .+}")
  @Produces(MediaType.WILDCARD)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getExecutionProxy(
    @ApiParam(value = "id", required = true) @PathParam("id") Integer id,
    @PathParam("path") final String param,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException, IOException {
    Execution execution = authorize(id);
    Response.ResponseBuilder responseBuilder = executionController.getExecutionProxy(execution, param, req);
    return responseBuilder.build();
    
  }
  
  @GET
  @Path("/{id}/tensorboard")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTensorBoardUrls(@PathParam("id") Integer id, @Context SecurityContext sc) throws JobException {
    Execution exec = authorize(id);
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      urls.addAll(executionController.getTensorBoardUrls(user, exec, job));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while getting TensorBoard endpoints" + e.getLocalizedMessage(), e);
    }
    
    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };
    
    return Response.ok().entity(listUrls).build();
  }
  
  
  private Execution authorize(Integer id) throws JobException {
    Execution execution = executionFacade.findById(id);
    if (execution == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, Level.FINE,
        "execution with id: " + id + " does not belong to job: " + job.getName() + " or does not exist");
    } else {
      if (!job.getExecutionCollection().contains(execution)) {
        throw new JobException(RESTCodes.JobErrorCode.UNAUTHORIZED_EXECUTION_ACCESS, Level.FINE);
      }
    }
    return execution;
  }
  
  
  public enum Action {
    START("start"),
    STOP("stop");
    
    private final String name;
    
    Action(String name) {
      this.name = name;
    }
    
    public static Action fromString(String name) {
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