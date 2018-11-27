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
import io.hops.hopsworks.api.jobs.executions.ExecutionService;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.Resource;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.AppInfoDTO;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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
public class JobService {
  
  private static final Logger LOGGER = Logger.getLogger(JobService.class.getName());
  
  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionService executions;
  @EJB
  private JobController jobController;
  @EJB
  private ExecutionController executionController;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private YarnApplicationAttemptStateFacade yarnApplicationAttemptStateFacade;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
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
  
  private static final String PROXY_USER_COOKIE_NAME = "proxy-user";
  
  @ApiOperation(value = "Get a list of all jobs for this project", response = JobDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
    @BeanParam Pagination pagination,
    @BeanParam JobsBeanParam jobsBeanParam,
    @Context UriInfo uriInfo) {
    Resource resource = new Resource(Resource.Name.JOBS);
    resource.setOffset(pagination.getOffset());
    resource.setLimit(pagination.getLimit());
    resource.setSort(jobsBeanParam.getSortBySet());
    resource.setFilter(jobsBeanParam.getFilter());
    resource.setExpansions(jobsBeanParam.getResources());
    
    JobDTO dto = jobsBuilder.build(uriInfo, resource, project);

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
    Resource resource = new Resource(Resource.Name.JOBS);
    resource.setExpansions(jobsBeanParam.getResources());
    JobDTO dto = jobsBuilder.build(uriInfo, resource, job);
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
    JobDTO dto = jobsBuilder.build(uriInfo, new Resource(Resource.Name.JOBS), job);
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
  public Response getJobUI(@PathParam("appId") String appId, @PathParam("isLivy") String isLivy) throws JobException {
    executionController.checkAccessRight(appId, project);
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    
    try {
      String trackingUrl = yarnApplicationAttemptStateFacade.findTrackingUrlByAppId(appId);
      if (trackingUrl != null && !trackingUrl.isEmpty()) {
        trackingUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
          + appId + "/prox/" + trackingUrl;
        urls.add(new YarnAppUrlsDTO("spark", trackingUrl));
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
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
      LOGGER.log(Level.SEVERE, "Exception while getting TensorBoard endpoints" + e.getLocalizedMessage(), e);
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
  public Response getYarnUI(@PathParam("appId") String appId) throws JobException {
    executionController.checkAccessRight(appId, project);
    
    try {
      String yarnUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
        + appId + "/prox/" + settings.getYarnWebUIAddress()
        + "/cluster/app/"
        + appId;
      
      return Response.ok().entity(yarnUrl).build();
      
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
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
  public Response getAppInfo(@PathParam("appId") String appId) throws JobException {
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
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
    }
    return Response.ok().status(Response.Status.NOT_FOUND).build();
  }
  
  private static final HashSet<String> PASS_THROUGH_HEADERS
    = new HashSet<String>(
    Arrays
      .asList("User-Agent", "user-agent", "Accept", "accept",
        "Accept-Encoding", "accept-encoding",
        "Accept-Language",
        "accept-language",
        "Accept-Charset", "accept-charset"));
  
  /**
   * Get the job ui for the specified job.
   * This act as a proxy to get the job ui from yarn
   * <p>
   * @param appId
   * @param param
   * @param req
   * @return
   */
  @GET
  @Path("/{appId}/prox/{path: .+}")
  @Produces(MediaType.WILDCARD)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getProxy(@PathParam("appId") final String appId, @PathParam("path") final String param,
    @Context HttpServletRequest req) throws JobException {
  
    executionController.checkAccessRight(appId, project);
    try {
      String trackingUrl;
      if (param.matches("http([a-zA-Z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
        trackingUrl = param;
      } else {
        trackingUrl = "http://" + param;
      }
      trackingUrl = trackingUrl.replace("@hwqm", "?");
      if (!hasAppAccessRight(trackingUrl)) {
        LOGGER.log(Level.SEVERE,
          "A user is trying to access an app outside their project!");
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      org.apache.commons.httpclient.URI uri
        = new org.apache.commons.httpclient.URI(trackingUrl, false);
      
      HttpClientParams params = new HttpClientParams();
      params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
      params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
        true);
      HttpClient client = new HttpClient(params);
      
      final HttpMethod method = new GetMethod(uri.getEscapedURI());
      Enumeration<String> names = req.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        String value = req.getHeader(name);
        if (PASS_THROUGH_HEADERS.contains(name)) {
          //yarn does not send back the js if encoding is not accepted
          //but we don't want to accept encoding for the html because we
          //need to be able to parse it
          if (!name.toLowerCase().equals("accept-encoding") || trackingUrl.
            contains(".js")) {
            method.setRequestHeader(name, value);
          }
        }
      }
      String user = req.getRemoteUser();
      if (user != null && !user.isEmpty()) {
        method.setRequestHeader("Cookie", PROXY_USER_COOKIE_NAME + "="
          + URLEncoder.encode(user, "ASCII"));
      }
      
      client.executeMethod(method);
      Response.ResponseBuilder responseBuilder = Response.ok();
      for (Header header : method.getResponseHeaders()) {
        responseBuilder.header(header.getName(), header.getValue());
      }
      //method.getPath().contains("/allexecutors") is needed to replace the links under Executors tab
      //which are in a json response object
      if (method.getResponseHeader("Content-Type") == null || method.
        getResponseHeader("Content-Type").getValue().contains("html")
        || method.getPath().contains("/allexecutors")) {
        final String source = "http://" + method.getURI().getHost() + ":"
          + method.getURI().getPort();
        if (method.getResponseHeader("Content-Length") == null) {
          responseBuilder.entity(new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException,
              WebApplicationException {
              Writer writer
                = new BufferedWriter(new OutputStreamWriter(out));
              InputStream stream = method.getResponseBodyAsStream();
              Reader in = new InputStreamReader(stream, "UTF-8");
              char[] buffer = new char[4 * 1024];
              String remaining = "";
              int n;
              while ((n = in.read(buffer)) != -1) {
                StringBuilder strb = new StringBuilder();
                strb.append(buffer, 0, n);
                String s = remaining + strb.toString();
                remaining = s.substring(s.lastIndexOf(">") + 1, s.length());
                s = hopify(s.substring(0, s.lastIndexOf(">") + 1), param,
                  appId,
                  source);
                writer.write(s);
              }
              writer.flush();
            }
          });
        } else {
          String s = hopify(method.getResponseBodyAsString(), param, appId,
            source);
          responseBuilder.entity(s);
          responseBuilder.header("Content-Length", s.length());
        }
        
      } else {
        responseBuilder.entity(new StreamingOutput() {
          @Override
          public void write(OutputStream out) throws IOException,
            WebApplicationException {
            InputStream stream = method.getResponseBodyAsStream();
            org.apache.hadoop.io.IOUtils.copyBytes(stream, out, 4096, true);
            out.flush();
          }
        });
      }
      return responseBuilder.build();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
      return Response.ok().build();
    }
    
  }
  
  private String hopify(String ui, String param, String appId, String source) {
    
    //remove the link to the full cluster information in the yarn ui
    ui = ui.replaceAll(
      "<div id=\"user\">[\\s\\S]+Logged in as: dr.who[\\s\\S]+<div id=\"logo\">",
      "<div id=\"logo\">");
    ui = ui.replaceAll(
      "<tfoot>[\\s\\S]+</tfoot>",
      "");
    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td class=\"content\">",
      "<td class=\"content\">");
    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td ", "<td ");
    ui = ui.replaceAll(
      "<li><a ui-sref=\"submit\"[\\s\\S]+new Job</a></li>", "");
    
    ui = ui.replaceAll("(?<=(href|src)=.[^>]{0,200})\\?", "@hwqm");
    
    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-zA-Z])",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/"
        + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-zA-Z])",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/"
        + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/api/project/"
      + project.getId() + "/jobs/" + appId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/api/project/"
      + project.getId() + "/jobs/" + appId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=http)",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-zA-Z])",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/" + param);
    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/" + param);
    ui = ui.replaceAll("(?<=\"(stdout\"|stderr\") : \")(?=[a-zA-Z])",
      "/hopsworks-api/api/project/"
        + project.getId() + "/jobs/" + appId + "/prox/");
    ui = ui.replaceAll("here</a>\\s+for full log", "here</a> for latest " + settings.getSparkUILogsOffset()
      + " bytes of logs");
    ui = ui.replaceAll("/@hwqmstart=0", "/@hwqmstart=-" + settings.getSparkUILogsOffset());
    return ui;
  }
  
  private boolean hasAppAccessRight(String trackingUrl) {
    String appId = "";
    if (trackingUrl.contains("application_")) {
      for (String elem : trackingUrl.split("/")) {
        if (elem.contains("application_")) {
          appId = elem;
          break;
        }
      }
    } else if (trackingUrl.contains("container_")) {
      appId = "application_";
      for (String elem : trackingUrl.split("/")) {
        if (elem.contains("container_")) {
          String[] containerIdElem = elem.split("_");
          appId = appId + containerIdElem[2] + "_" + containerIdElem[3];
          break;
        }
      }
    } else if (trackingUrl.contains("appattempt_")) {
      appId = "application_";
      for (String elem : trackingUrl.split("/")) {
        if (elem.contains("appattempt_")) {
          String[] containerIdElem = elem.split("_");
          appId = appId + containerIdElem[1] + "_" + containerIdElem[2];
          break;
        }
      }
    } else {
      if (trackingUrl.contains("static")) {
        return true;
      }
      return false;
    }
    if (!appId.isEmpty()) {
      String appUser = yarnApplicationstateFacade.findByAppId(appId).
        getAppuser();
      if (!project.getName().equals(hdfsUsersBean.getProjectName(
        appUser))) {
        return false;
      }
    }
    return true;
  }
}
