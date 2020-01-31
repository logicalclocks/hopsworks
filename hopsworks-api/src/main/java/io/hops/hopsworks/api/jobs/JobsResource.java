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

package io.hops.hopsworks.api.jobs;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.executions.ExecutionsResource;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.jobs.AppInfoDTO;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.JobType;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobsResource {
  
  private static final Logger LOGGER = Logger.getLogger(JobsResource.class.getName());
  
  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionsResource executions;
  @EJB
  private JobController jobController;
  @EJB
  private ExecutionController executionController;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private YarnApplicationAttemptStateFacade yarnApplicationAttemptStateFacade;
  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private JobsBuilder jobsBuilder;
  
  private Project project;
  public JobsResource setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }
  
  @ApiOperation(value = "Get a list of all jobs for this project", response = JobDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam JobsBeanParam jobsBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.JOBS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(jobsBeanParam.getSortBySet());
    resourceRequest.setFilter(jobsBeanParam.getFilter());
    resourceRequest.setExpansions(jobsBeanParam.getExpansions().getResources());
    
    JobDTO dto = jobsBuilder.build(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get the job with requested ID", response = JobDTO.class)
  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getJob(@PathParam("name") String name,
    @BeanParam JobsBeanParam jobsBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws JobException {
    Jobs job = jobController.getJob(project, name);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.JOBS);
    resourceRequest.setExpansions(jobsBeanParam.getExpansions().getResources());
    JobDTO dto = jobsBuilder.build(uriInfo, resourceRequest, job);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation( value = "Create or Update a Job.", response = JobDTO.class)
  @PUT
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response put (
    @ApiParam(value = "Job configuration parameters", required = true) JobConfiguration config,
    @ApiParam(value = "name", required = true) @PathParam("name") String name,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws JobException {
    if (config == null) {
      throw new IllegalArgumentException("Job configuration was not provided.");
    }
    
    Users user = jWTHelper.getUserPrincipal(sc);
    if (Strings.isNullOrEmpty(config.getAppName())) {
      config.setAppName(name);
    }
    if (!HopsUtils.jobNameValidator(config.getAppName(), Settings.FILENAME_DISALLOWED_CHARS)) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NAME_INVALID, Level.FINE, "job name: " + config.getAppName());
    }
    //Check if job with same name exists so we update it instead of creating it
    Response.Status status = Response.Status.CREATED;
    Jobs job = jobFacade.findByProjectAndName(project, config.getAppName());
    if(job != null) {
      status = Response.Status.OK;
    }
    job = jobController.putJob(user, project, job, config);
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.JOBS), job);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Integer.toString(dto.getId()));
    if(status == Response.Status.CREATED) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation(value = "Delete the job with the given ID")
  @DELETE
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
    @ApiParam(value = "id", required = true) @PathParam("name") String name,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Jobs job = jobController.getJob(project, name);
    
    if(job.getJobConfig().getSchedule() != null) {
      jobController.unscheduleJob(job);
    }
    switch (job.getJobType()) {
      case SPARK:
      case PYSPARK:
      case FLINK:
      case BEAM_FLINK:
        jobController.deleteJob(job, user);
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
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateSchedule(ScheduleDTO schedule,
    @PathParam("name") String name,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo) throws JobException {
    
    if(schedule == null){
      throw new IllegalArgumentException("Schedule parameter was not provided.");
    }
    Jobs job = jobController.getJob(project, name);
    
    Users user = jWTHelper.getUserPrincipal(sc);
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
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response unscheduleJob(@PathParam("name") String name, @Context SecurityContext sc) throws JobException {
    if(Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("job name was not provided or it was not set.");
    }
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    jobController.unscheduleJob(job);
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Inspect Spark user program and return SparkJobConfiguration",
    response = SparkJobConfiguration.class)
  @GET
  @Path("{jobtype : spark|pyspark|flink}/inspection")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response inspect (
    @ApiParam (value = "spark job type", example = "spark") @PathParam("jobtype") JobType jobtype,
    @ApiParam(value = "path", example = "/Projects/demo_spark_admin000/Resources/spark-examples.jar",
      required = true)  @QueryParam("path") String path,
    @Context SecurityContext sc) throws JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    JobConfiguration config = jobController.inspectProgram(path, project, user, jobtype);
    return Response.ok().entity(config).build();
  }
  
  @Path("{name}/executions")
  public ExecutionsResource executions(@PathParam("name") String name) throws JobException {
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
 
  
  //====================================================================================================================
  // Jobs & Notebooks proxy endpoints
  //====================================================================================================================
  
  /**
   * Get the Job UI url for the specified job
   * <p>
   * @param appId
   * @param isLivy
   * @return url
   */
  @GET
  @Path("/{appId}/ui/{isLivy}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getJobUI(@PathParam("appId") String appId, @PathParam("isLivy") String isLivy,
    @Context SecurityContext sc) throws JobException {
    executionController.checkAccessRight(appId, project);
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    
    try {
      String trackingUrl = yarnApplicationAttemptStateFacade.findTrackingUrlByAppId(appId);
      if (trackingUrl != null && !trackingUrl.isEmpty()) {
        trackingUrl = "/hopsworks-api/yarnui/" + trackingUrl;
        urls.add(new YarnAppUrlsDTO("spark", trackingUrl));
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while getting job ui " + e.getLocalizedMessage(), e);
    }
    
    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };
    
    return Response.ok().entity(listUrls).build();
  }
  
  /**
   * Get the Job UI url for the specified job
   * <p>
   * @param appId
   * @param sc
   * @return url
   */
  @GET
  @Path("/{appId}/tensorboard")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTensorBoardUrls(@PathParam("appId") String appId,
    @Context SecurityContext sc)
    throws JobException {
    executionController.checkAccessRight(appId, project);
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    Users user = jWTHelper.getUserPrincipal(sc);
    try {
      urls.addAll(executionController.getTensorBoardUrls(user, appId, project));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while getting TensorBoard endpoints " + e.getLocalizedMessage(), e);
    }
    
    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };
    
    return Response.ok().entity(listUrls).build();
  }
  
  /**
   * Get the Yarn UI url for the specified job
   * <p>
   * @param appId
   * @return url
   */
  @GET
  @Path("/{appId}/yarnui")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getYarnUI(@PathParam("appId") String appId, @Context SecurityContext sc) throws JobException {
    executionController.checkAccessRight(appId, project);
    
    try {
      String yarnUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
        + appId + "/prox/" + settings.getYarnWebUIAddress() + "/cluster/app/" + appId;
      
      return Response.ok().entity(yarnUrl).build();
      
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while getting job ui " + e.getLocalizedMessage(), e);
    }
    return Response.ok().build();
  }
  
  /**
   * Get application run info for the specified job
   * <p>
   * @param appId
   * @return url
   */
  @GET
  @Path("/{appId}/appinfo")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAppInfo(@PathParam("appId") String appId, @Context SecurityContext sc) throws JobException {
    executionController.checkAccessRight(appId, project);
    Execution execution = executionFacade.findByAppId(appId);
    try {
      long startTime = System.currentTimeMillis() - 60000;
      long endTime = System.currentTimeMillis();
      boolean running = true;
      if (execution != null) {
        startTime = execution.getSubmissionTime().getTime();
        endTime = startTime + execution.getExecutionDuration();
        running = false;
        if (!execution.getState().isFinalState()) {
          running = true;
        }
      }
    
      InfluxDB influxDB = InfluxDBFactory.connect(settings.
        getInfluxDBAddress(), settings.getInfluxDBUser(), settings.
        getInfluxDBPW());
    
      // Transform application_1493112123688_0001 to 1493112123688_0001
      // application_ = 12 chars
      String timestamp_attempt = appId.substring(12);
    
      Query query = new Query("show tag values from nodemanager with key=\"source\" " + "where source =~ /^.*"
        + timestamp_attempt + ".*$/", "graphite");
      QueryResult queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
    
      int nbExecutors = 0;
      HashMap<Integer, List<String>> executorInfo = new HashMap<>();
      int index = 0;
      if (queryResult != null && queryResult.getResults() != null) {
        for (QueryResult.Result res : queryResult.getResults()) {
          if (res.getSeries() != null) {
            for (QueryResult.Series series : res.getSeries()) {
              List<List<Object>> values = series.getValues();
              if (values != null) {
                nbExecutors += values.size();
                for (List<Object> l : values) {
                  executorInfo.put(index, Stream.of(Objects.toString(l.get(1))).collect(Collectors.toList()));
                  index++;
                }
              }
            }
          }
        }
      }
    
      /*
       * At this point executor info contains the keys and a list with a single value, the YARN container id
       */
      String vCoreTemp = null;
      HashMap<String, String> hostnameVCoreCache = new HashMap<>();
    
      for (Map.Entry<Integer, List<String>> entry : executorInfo.entrySet()) {
        query = new Query("select MilliVcoreUsageAvgMilliVcores, hostname from nodemanager where source = \'" + entry.
          getValue().get(0) + "\' limit 1", "graphite");
        queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
      
        if (queryResult != null && queryResult.getResults() != null
          && queryResult.getResults().get(0) != null && queryResult.
          getResults().get(0).getSeries() != null) {
          List<List<Object>> values = queryResult.getResults().get(0).getSeries().get(0).getValues();
          String hostname = Objects.toString(values.get(0).get(2)).split("=")[1];
          entry.getValue().add(hostname);
        
          if (!hostnameVCoreCache.containsKey(hostname)) {
            // Not in cache, get the vcores of the host machine
            query = new Query("select AllocatedVCores+AvailableVCores from nodemanager " + "where hostname =~ /.*"
              + hostname + ".*/ limit 1", "graphite");
            queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
          
            if (queryResult != null && queryResult.getResults() != null
              && queryResult.getResults().get(0) != null && queryResult.
              getResults().get(0).getSeries() != null) {
              values = queryResult.getResults().get(0).getSeries().get(0).getValues();
              vCoreTemp = Objects.toString(values.get(0).get(1));
              entry.getValue().add(vCoreTemp);
              hostnameVCoreCache.put(hostname, vCoreTemp); // cache it
            }
          } else {
            // It's a hit, skip the database query
            entry.getValue().add(hostnameVCoreCache.get(hostname));
          }
        }
      }
    
      influxDB.close();
    
      AppInfoDTO appInfo = new AppInfoDTO(appId, startTime,
        running, endTime, nbExecutors, executorInfo);
    
      return Response.ok().entity(appInfo).build();
    
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception while getting job ui " + e.getLocalizedMessage(), e);
    }
    return Response.ok().status(Response.Status.NOT_FOUND).build();
  }
  
}
