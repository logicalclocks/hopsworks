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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;


@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobService {
  
  private static final Logger LOGGER = Logger.getLogger(JobService.class.getName());
  
  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionService executions;
  @EJB
  private JobController jobController;
  @EJB
  private SparkController sparkController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private JobsBuilder jobsBuilder;
  
  
  private Project project;
  public JobService setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }
  
  
  @ApiOperation(value = "Get a list of all jobs for this project", response = JobDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam JobsBeanParam jobsBeanParam,
    @Context UriInfo uriInfo) {
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS,
      pagination.getOffset(), pagination.getLimit(), jobsBeanParam.getSortBySet(), jobsBeanParam.getFilter(),
        jobsBeanParam.getExpand()), project);

    GenericEntity<JobDTO> ge
      = new GenericEntity<JobDTO>(dto) { };
    
    return Response.ok().entity(ge).build();
  }
  
  @ApiOperation(value = "Get the job with requested ID", response = JobDTO.class)
  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getJob(@PathParam("name") String name,
    @BeanParam JobsBeanParam jobsBeanParam,
    @Context UriInfo uriInfo) throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS,
      jobsBeanParam.getExpand()), job);
    GenericEntity<JobDTO> ge = new GenericEntity<JobDTO>(dto) { };
    return Response.ok().entity(ge).build();
  }
  
  
  @ApiOperation( value = "Create a Job", response = JobDTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response create (
    YarnJobConfiguration config,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException {
    if (config == null) {
      throw new IllegalArgumentException("Job configuration was not provided.");
    }
    
    Users user = jWTHelper.getUserPrincipal(req);
    if (Strings.isNullOrEmpty(config.getAppName())) {
      throw new IllegalArgumentException("Job name was not provided.");
    } else if (!HopsUtils.jobNameValidator(config.getAppName(), Settings.FILENAME_DISALLOWED_CHARS)) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NAME_INVALID, Level.FINE, "job name: " + config.getAppName());
    }
    
    Jobs job = jobController.createJob(user, project, config);
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS), job);
    GenericEntity<JobDTO> ge = new GenericEntity<JobDTO>(dto) {};
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Integer.toString(dto.getId()));
    return Response.created(builder.build()).entity(ge).build();
    
  }
  
  @ApiOperation(value = "Delete the job with the given ID")
  @DELETE
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
    @ApiParam(value = "id", required = true) @PathParam("name") String name,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException {
    Users user = jWTHelper.getUserPrincipal(req);
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    
    if(job.getJobConfig().getSchedule() != null) {
      jobController.unscheduleJob(job);
    }
    switch (job.getJobType()) {
      case SPARK:
      case PYSPARK:
        sparkController.deleteJob(job, user);
        break;
      default:
        throw new JobException(RESTCodes.JobErrorCode.JOB_TYPE_UNSUPPORTED, Level.FINEST, job.getJobType().toString());
    }
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Create/Update job's schedule.")
  @PUT
  @Path("{name}/schedule")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateSchedule(ScheduleDTO schedule,
    @PathParam("name") String name,
    @Context HttpServletRequest req) throws JobException {
    if(Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("job name was not provided or it was empty.");
    }
    if(schedule == null){
      throw new IllegalArgumentException("Schedule parameter was null.");
    }
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "jobId:"+name);
    }
    Users user = jWTHelper.getUserPrincipal(req);
    jobController.updateSchedule(project, job, schedule, user);
    return Response.noContent().build();
  }
  
  /**
   * Remove scheduling for the job with this jobid. The return value is a
   * JSON object stating operation successful
   * or not.
   * <p>
   * @param name job name
   * @return Response
   */
  @ApiOperation(value = "Cancel a job's schedule.")
  @DELETE
  @Path("{name}/schedule")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unscheduleJob(@PathParam("name") String name) throws JobException {
    if(Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("job name was not provided or it was empty.");
    }
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Inspect Spark user program and return SparkJobConfiguration",
    response = SparkJobConfiguration.class)
  @GET
  @Path("{jobtype : spark|pyspark|flink}/inspection")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response inspect (
    @ApiParam (value = "spark job type", example = "spark") @PathParam("jobtype") JobType jobtype,
    @ApiParam(value = "path", example = "/Projects/demo_spark_admin000/Resources/spark-examples.jar",
      required = true)  @QueryParam("path") String path,
    @Context HttpServletRequest req) throws JobException {
    Users user = jWTHelper.getUserPrincipal(req);
    JobConfiguration config = jobController.inspectProgram(path, project, user, jobtype);
    return Response.ok().entity(config).build();
  }
  
  @Path("{name}/executions")
  public ExecutionService executions(@PathParam("name") String name) throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "job name:" + name);
    } else {
      return this.executions.setJob(job);
    }
  }
  
  public enum Action {
    INSPECT
  }
  
}
