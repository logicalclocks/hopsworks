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
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.elasticsearch.common.Strings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * <p>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobService {

  private static final Logger LOGGER = Logger.getLogger(JobService.class.getName());

  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionService executions;
  @Inject
  private SparkService spark;
  @Inject
  private FlinkService flink;
  @EJB
  private JobController jobController;
  @EJB
  private SparkController sparkController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private JobsBuilder jobsBuilder;
  

  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
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
    @QueryParam("offset") Integer offset,
    @QueryParam("limit") Integer limit,
    @QueryParam("sort_by") ResourceProperties.SortBy sortBy,
    @QueryParam("order_by") ResourceProperties.OrderBy orderBy,
    @QueryParam("expand") String expand,
    @ApiParam(value = "Type of job, i.e. spark, flink") JobType type,
    @Context UriInfo uriInfo) {
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS, offset, limit,
      sortBy, orderBy, expand), project);
    
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
    @QueryParam("expand") String expand,
    @Context UriInfo uriInfo) throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS, expand), job);
    GenericEntity<JobDTO> ge = new GenericEntity<JobDTO>(dto) { };
    return Response.ok().entity(ge).build();
  }
  
  
  @ApiOperation( value = "Create Job", response = JobDTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response create (
    JobConfiguration config,
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
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
    @ApiParam(value = "id", required = true) @PathParam("name") String name,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException {
    LOGGER.log(Level.FINE, "Request to delete job with id:" + name);
    Users user = jWTHelper.getUserPrincipal(req);
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    
    //Data Scientists should be able to delete only the jobs they created.
    String projectRole = projectTeamFacade.findCurrentRole(project, user);
    if(!projectRole.equals(AllowedProjectRoles.DATA_SCIENTIST)){
      throw new JobException(RESTCodes.JobErrorCode.JOB_DELETION_FORBIDDEN, Level.FINEST,
        "Your role in project: " + project.getName() + " is: " +  projectRole);
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
  @POST
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
    return Response.ok().build();
  }
  
  /**
   * Remove scheduling for the job with this jobid. The return value is a
   * JSON object stating operation successful
   * or not.
   * <p>
   * @param name
   * @return
   */
  @ApiOperation(value = "Cancel a job's schedule.")
  @DELETE
  @Path("/{name}/schedule")
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
    
    return Response.ok().build();
  }
  
  @Path("/spark")
  public SparkService spark() {
    return this.spark.setProject(project);
  }
  
  @Path("/pyspark")
  public SparkService pyspark() {
    return this.spark.setProject(project);
  }
  
  @Path("/flink")
  public FlinkService flink() {
    return this.flink.setProject(project);
  }
  
  @Path("/{name}/executions")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public ExecutionService executions(@PathParam("name") String name) throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "job name:" + name);
    } else {
      return this.executions.setJob(job);
    }
  }

}
