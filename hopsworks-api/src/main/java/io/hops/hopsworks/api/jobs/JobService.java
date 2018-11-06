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
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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
  @Path("{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getJob(@PathParam("jobId") int jobId,
    @QueryParam("expand") String expand,
    @Context UriInfo uriInfo) throws JobException {
    Jobs job = jobFacade.findByProjectAndId(project, jobId);
    if(job == null){
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST);
    }
    JobDTO dto = jobsBuilder.build(uriInfo, new ResourceProperties(ResourceProperties.Name.JOBS, expand), job);
    GenericEntity<JobDTO> ge = new GenericEntity<JobDTO>(dto) { };
    return Response.ok().entity(ge).build();
  }
  
  
  @ApiOperation(value = "Delete the job with the given ID")
  @DELETE
  @Path("/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
    @ApiParam(value = "id", required = true) @PathParam("jobId") int jobId,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws JobException {
    LOGGER.log(Level.FINE, "Request to delete job with id:" + jobId);
    Users user = jWTHelper.getUserPrincipal(req);
    Jobs job = jobFacade.findByProjectAndId(project, jobId);
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
  @Path("{jobId}/schedule")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response updateSchedule(ScheduleDTO schedule, @PathParam("jobId") Integer jobId,
    @Context HttpServletRequest req) throws JobException {
    if(jobId == null || jobId < 0) {
      throw new IllegalArgumentException("jobId parameter was not provided or it was negative.");
    }
    if(schedule == null){
      throw new IllegalArgumentException("Schedule parameter was null.");
    }
    Jobs job = jobFacade.findByProjectAndId(project, jobId);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "jobId:"+jobId);
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
   * @param jobId
   * @return
   */
  @ApiOperation(value = "Cancel a job's schedule.")
  @DELETE
  @Path("/{jobId}/schedule")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unscheduleJob(@PathParam("jobId") Integer jobId) throws JobException {
    if(jobId == null || jobId < 0) {
      throw new IllegalArgumentException("jobId parameter was not provided or it was negative.");
    }
    Jobs job = jobFacade.findByProjectAndId(project, jobId);
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
  
  @Path("/{jobId}/executions")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public ExecutionService executions(@PathParam("jobId") int jobId) throws JobException {
    Jobs job = jobFacade.findByProjectAndId(project, jobId);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "jobId:" + jobId);
    } else {
      return this.executions.setJob(job);
    }
  }
  
  
  /**
   * Get the JobConfiguration object for the specified job. The sole reason of
   * existence of this method is the dodginess
   * of polymorphism in JAXB/JAXRS. As such, the jobConfig field is always empty
   * when a Jobs object is
   * returned. This method must therefore be called explicitly to get the job
   * configuration.
   * <p>
   * @param jobId
   * @return
   */
//  @GET
//  @Path("/{jobId}/config")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getJobConfiguration(@PathParam("jobId") int jobId) {
//    Jobs job = jobFacade.findByProjectAndId(project, jobId);
//    if (job == null) {
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    } else {
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(job.getJobConfig()).build();
//    }
//  }

  /**
   * Get the appId for the specified job
   * <p>
   * @param jobId
   * @return url
   */
//  @GET
//  @Path("/{jobId}/appId")
//  @Produces(MediaType.TEXT_PLAIN)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getAppId(@PathParam("jobId") int jobId) {
//    Jobs job = jobFacade.findByProjectAndId(project, jobId);
//    if (job == null) {
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    } else {
//      Execution execution = exeFacade.findForJob(job).get(0);
//      if (execution == null) {
//        LOGGER.log(Level.SEVERE, "No job execution found for job {}", job.getName());
//        return Response.status(Response.Status.NOT_FOUND).build();
//      }
//      Execution updatedExecution = exeFacade.getExecution(execution.getJob().getId());
//      if (updatedExecution != null) {
//        execution = updatedExecution;
//      }
//
//      try {
//        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(execution.getAppId()).build();
//      } catch (Exception e) {
//        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//      }
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    }
//  }

//  /**
//   * Get all the appIds for the specified job
//   * <p>
//   * @param jobId
//   * @return url
//   */
//  @GET
//  @Path("/{jobId}/appIds")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getAppIds(@PathParam("jobId") int jobId) {
//    Jobs job = jobFacade.findByProjectAndId(project, jobId);
//    if (job == null) {
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    } else {
//      List<Execution> executions = exeFacade.findForJob(job);
//      if (executions == null || executions.isEmpty()) {
//        LOGGER.log(Level.SEVERE, "No job execution found for job {}", job.getName());
//        return Response.status(Response.Status.NOT_FOUND).build();
//      }
//
//      try {
//        List<AppIdDTO> appIdStrings = new ArrayList<>();
//        for (Execution ex : executions) {
//          appIdStrings.add(new AppIdDTO(ex.getAppId()));
//        }
//
//        GenericEntity<List<AppIdDTO>> appIds = new GenericEntity<List<AppIdDTO>>(appIdStrings) { };
//        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(appIds).build();
//
//      } catch (Exception e) {
//        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//      }
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    }
//  }
//
//  private String getHdfsUser(HttpServletRequest req) {
//    Users user = jWTHelper.getUserPrincipal(req);
//    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
//
//    return hdfsUsername;
//  }

  

//  private List<YarnAppUrlsDTO> getTensorBoardUrls(String hdfsUser, String appId) throws JobException {
//    List<YarnAppUrlsDTO> urls = new ArrayList<>();
//
//    DistributedFileSystemOps client = null;
//
//    try {
//      client = dfs.getDfsOps(hdfsUser);
//      FileStatus[] statuses = client.getFilesystem().globStatus(new org.apache.hadoop.fs.Path("/Projects/" + project.
//          getName() + "/Experiments/" + appId + "/TensorBoard.*"));
//      DistributedFileSystem fs = client.getFilesystem();
//      for (FileStatus status : statuses) {
//        LOGGER.log(Level.FINE, "Reading tensorboard for: {0}", status.getPath());
//        FSDataInputStream in = null;
//        try {
//          in = fs.open(new org.apache.hadoop.fs.Path(status.getPath().toString()));
//          String url = IOUtils.toString(in, "UTF-8");
//          int prefix = url.indexOf("http://");
//          if (prefix != -1) {
//            url = url.substring("http://".length());
//          }
//          String name = status.getPath().getName();
//          urls.add(new YarnAppUrlsDTO(name, url));
//        } catch (Exception e) {
//          LOGGER.log(Level.WARNING, "Problem reading file with tensorboard address from HDFS: " + e.getMessage());
//        } finally {
//          org.apache.hadoop.io.IOUtils.closeStream(in);
//        }
//
//      }
//    } catch (Exception e) {
//      throw new JobException(RESTCodes.JobErrorCode.TENSORBOARD_ERROR, Level.SEVERE, null, e.getMessage(), e);
//    } finally {
//      if (client != null) {
//        dfs.closeDfsClient(client);
//      }
//    }
//
//    return urls;
//  }


  /**
   * Get the Job UI url for the specified job
   * <p>
   * @param appId
   * @param isLivy
   * @return url
   */
//  @GET
//  @Path("/{appId}/ui/{isLivy}")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getJobUI(@PathParam("appId") String appId, @PathParam("isLivy") String isLivy) {
//    Response noAccess = checkAccessRight(appId);
//    if (noAccess != null) {
//      return noAccess;
//    }
//    Response.Status response = Response.Status.OK;
//    List<YarnAppUrlsDTO> urls = new ArrayList<>();
//
//    try {
//      String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(appId);
//      if (trackingUrl != null && !trackingUrl.isEmpty()) {
//        trackingUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
//            + appId + "/prox/" + trackingUrl;
//        urls.add(new YarnAppUrlsDTO("spark", trackingUrl));
//      }
//    } catch (Exception e) {
//      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//    }
//
//    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };
//
//    return noCacheResponse.getNoCacheResponseBuilder(response).entity(listUrls).build();
//  }

  /**
   * Get the Job UI url for the specified job
   * <p>
   * @param appId
   * @param req
   * @return url
   */
//  @GET
//  @Path("/{appId}/tensorboard")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getTensorBoardUrls(@PathParam("appId") String appId, @Context HttpServletRequest req)  {
//    Response noAccess = checkAccessRight(appId);
//    if (noAccess != null) {
//      return noAccess;
//    }
//    Response.Status response = Response.Status.OK;
//    List<YarnAppUrlsDTO> urls = new ArrayList<>();
//    String hdfsUser = getHdfsUser(req);
//
//    try {
//      urls.addAll(getTensorBoardUrls(hdfsUser, appId));
//    } catch (Exception e) {
//      LOGGER.log(Level.SEVERE, "Exception while getting TensorBoard endpoints" + e.getLocalizedMessage(), e);
//    }
//
//    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };
//
//    return noCacheResponse.getNoCacheResponseBuilder(response).entity(listUrls).build();
//  }

//  private Response checkAccessRight(String appId) {
//    YarnApplicationstate appState = appStateFacade.findByAppId(appId);
//
//    if (appState == null) {
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    } else if (!hdfsUsersBean.getProjectName(appState.getAppuser()).equals(project.getName())) {
//      //In this case, a user is trying to access a job outside its project!!!
//      LOGGER.log(Level.SEVERE, "A user is trying to access a job outside their project!");
//      return Response.status(Response.Status.FORBIDDEN).build();
//    } else {
//      return null;
//    }
//  }

  /**
   * Get the Yarn UI url for the specified job
   * <p>
   * @param appId
   * @return url
   */
//  @GET
//  @Path("/{appId}/yarnui")
//  @Produces(MediaType.TEXT_PLAIN)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getYarnUI(@PathParam("appId") String appId) {
//    Response response = checkAccessRight(appId);
//    if (response != null) {
//      return response;
//    } else {
//
//      try {
//        String yarnUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
//            + appId + "/prox/" + settings.getYarnWebUIAddress()
//            + "/cluster/app/"
//            + appId;
//
//        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(yarnUrl).build();
//
//      } catch (Exception e) {
//        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//      }
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    }
//  }

  /**
   * Get application run info for the specified job
   * <p>
   * @param appId
   * @return url
   */
//  @GET
//  @Path("/{appId}/appinfo")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getAppInfo(@PathParam("appId") String appId) {
//    Response response = checkAccessRight(appId);
//    if (response != null) {
//      return response;
//    } else {
//      Execution execution = exeFacade.findByAppId(appId);
//      try {
//        long startTime = System.currentTimeMillis() - 60000;
//        long endTime = System.currentTimeMillis();
//        boolean running = true;
//        if (execution != null) {
//          startTime = execution.getSubmissionTime().getTime();
//          endTime = startTime + execution.getExecutionDuration();
//          running = false;
//          if (!execution.getState().isFinalState()) {
//            running = true;
//          }
//        }
//
//        InfluxDB influxDB = InfluxDBFactory.connect(settings.
//            getInfluxDBAddress(), settings.getInfluxDBUser(), settings.
//            getInfluxDBPW());
//
//        // Transform application_1493112123688_0001 to 1493112123688_0001
//        // application_ = 12 chars
//        String timestamp_attempt = appId.substring(12);
//
//        Query query = new Query("show tag values from nodemanager with key=\"source\" " + "where source =~ /^.*"
//            + timestamp_attempt + ".*$/", "graphite");
//        QueryResult queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
//
//        int nbExecutors = 0;
//        HashMap<Integer, List<String>> executorInfo = new HashMap<>();
//        int index=0;
//        if (queryResult != null && queryResult.getResults() != null){
//          for(QueryResult.Result res: queryResult.getResults()){
//            if(res.getSeries()!=null){
//              for(QueryResult.Series series : res.getSeries()){
//                List<List<Object>> values = series.getValues();
//                if(values!=null){
//                  nbExecutors += values.size();
//                  for(List<Object> l: values){
//                    executorInfo.put(index,Stream.of(Objects.toString(l.get(1))).collect(Collectors.toList()));
//                    index++;
//                  }
//                }
//              }
//            }
//          }
//        }
//
//        /*
//         * At this point executor info contains the keys and a list with a single value, the YARN container id
//         */
//        String vCoreTemp = null;
//        HashMap<String, String> hostnameVCoreCache = new HashMap<>();
//
//        for (Map.Entry<Integer, List<String>> entry : executorInfo.entrySet()) {
//          query = new Query("select MilliVcoreUsageAvgMilliVcores, hostname from nodemanager where source = \'" + entry.
//              getValue().get(0) + "\' limit 1", "graphite");
//          queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
//
//          if (queryResult != null && queryResult.getResults() != null
//              && queryResult.getResults().get(0) != null && queryResult.
//              getResults().get(0).getSeries() != null) {
//            List<List<Object>> values = queryResult.getResults().get(0).getSeries().get(0).getValues();
//            String hostname = Objects.toString(values.get(0).get(2)).split("=")[1];
//            entry.getValue().add(hostname);
//
//            if (!hostnameVCoreCache.containsKey(hostname)) {
//              // Not in cache, get the vcores of the host machine
//              query = new Query("select AllocatedVCores+AvailableVCores from nodemanager " + "where hostname =~ /.*"
//                  + hostname + ".*/ limit 1", "graphite");
//              queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
//
//              if (queryResult != null && queryResult.getResults() != null
//                  && queryResult.getResults().get(0) != null && queryResult.
//                  getResults().get(0).getSeries() != null) {
//                values = queryResult.getResults().get(0).getSeries().get(0).getValues();
//                vCoreTemp = Objects.toString(values.get(0).get(1));
//                entry.getValue().add(vCoreTemp);
//                hostnameVCoreCache.put(hostname, vCoreTemp); // cache it
//              }
//            } else {
//              // It's a hit, skip the database query
//              entry.getValue().add(hostnameVCoreCache.get(hostname));
//            }
//          }
//        }
//
//        influxDB.close();
//
//        AppInfoDTO appInfo = new AppInfoDTO(appId, startTime,
//            running, endTime, nbExecutors, executorInfo);
//
//        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(appInfo).build();
//
//      } catch (Exception e) {
//        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//      }
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    }
//  }

//  private static final HashSet<String> PASS_THROUGH_HEADERS
//      = new HashSet<String>(
//          Arrays
//              .asList("User-Agent", "user-agent", "Accept", "accept",
//                  "Accept-Encoding", "accept-encoding",
//                  "Accept-Language",
//                  "accept-language",
//                  "Accept-Charset", "accept-charset"));

  /**
   * Get the job ui for the specified job.
   * This act as a proxy to get the job ui from yarn
   * <p>
   * @param appId
   * @param param
   * @param req
   * @return
   */
//  @GET
//  @Path("/{appId}/prox/{path: .+}")
//  @Produces(MediaType.WILDCARD)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getProxy(@PathParam("appId") final String appId, @PathParam("path") final String param,
//      @Context HttpServletRequest req) {
//
//    Response response = checkAccessRight(appId);
//    if (response != null) {
//      return response;
//    }
//    try {
//      String trackingUrl;
//      if (param.matches("http([a-zA-Z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
//        trackingUrl = param;
//      } else {
//        trackingUrl = "http://" + param;
//      }
//      trackingUrl = trackingUrl.replace("@hwqm", "?");
//      if (!hasAppAccessRight(trackingUrl)) {
//        LOGGER.log(Level.SEVERE,
//            "A user is trying to access an app outside their project!");
//        return Response.status(Response.Status.FORBIDDEN).build();
//      }
//      org.apache.commons.httpclient.URI uri
//          = new org.apache.commons.httpclient.URI(trackingUrl, false);
//
//      HttpClientParams params = new HttpClientParams();
//      params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
//      params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
//          true);
//      HttpClient client = new HttpClient(params);
//
//      final HttpMethod method = new GetMethod(uri.getEscapedURI());
//      Enumeration<String> names = req.getHeaderNames();
//      while (names.hasMoreElements()) {
//        String name = names.nextElement();
//        String value = req.getHeader(name);
//        if (PASS_THROUGH_HEADERS.contains(name)) {
//          //yarn does not send back the js if encoding is not accepted
//          //but we don't want to accept encoding for the html because we
//          //need to be able to parse it
//          if (!name.toLowerCase().equals("accept-encoding") || trackingUrl.
//              contains(".js")) {
//            method.setRequestHeader(name, value);
//          }
//        }
//      }
//      String user = req.getRemoteUser();
//      if (user != null && !user.isEmpty()) {
//        method.setRequestHeader("Cookie", PROXY_USER_COOKIE_NAME + "="
//            + URLEncoder.encode(user, "ASCII"));
//      }
//
//      client.executeMethod(method);
//      Response.ResponseBuilder responseBuilder = noCacheResponse.
//          getNoCacheResponseBuilder(Response.Status.OK);
//      for (Header header : method.getResponseHeaders()) {
//        responseBuilder.header(header.getName(), header.getValue());
//      }
//      //method.getPath().contains("/allexecutors") is needed to replace the links under Executors tab
//      //which are in a json response object
//      if (method.getResponseHeader("Content-Type") == null || method.
//          getResponseHeader("Content-Type").getValue().contains("html")
//          || method.getPath().contains("/allexecutors")) {
//        final String source = "http://" + method.getURI().getHost() + ":"
//            + method.getURI().getPort();
//        if (method.getResponseHeader("Content-Length") == null) {
//          responseBuilder.entity(new StreamingOutput() {
//            @Override
//            public void write(OutputStream out) throws IOException,
//                WebApplicationException {
//              Writer writer
//                  = new BufferedWriter(new OutputStreamWriter(out));
//              InputStream stream = method.getResponseBodyAsStream();
//              Reader in = new InputStreamReader(stream, "UTF-8");
//              char[] buffer = new char[4 * 1024];
//              String remaining = "";
//              int n;
//              while ((n = in.read(buffer)) != -1) {
//                StringBuilder strb = new StringBuilder();
//                strb.append(buffer, 0, n);
//                String s = remaining + strb.toString();
//                remaining = s.substring(s.lastIndexOf(">") + 1, s.length());
//                s = hopify(s.substring(0, s.lastIndexOf(">") + 1), param,
//                    appId,
//                    source);
//                writer.write(s);
//              }
//              writer.flush();
//            }
//          });
//        } else {
//          String s = hopify(method.getResponseBodyAsString(), param, appId,
//              source);
//          responseBuilder.entity(s);
//          responseBuilder.header("Content-Length", s.length());
//        }
//
//      } else {
//        responseBuilder.entity(new StreamingOutput() {
//          @Override
//          public void write(OutputStream out) throws IOException,
//              WebApplicationException {
//            InputStream stream = method.getResponseBodyAsStream();
//            org.apache.hadoop.io.IOUtils.copyBytes(stream, out, 4096, true);
//            out.flush();
//          }
//        });
//      }
//      return responseBuilder.build();
//    } catch (Exception e) {
//      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.getLocalizedMessage(), e);
//      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
//    }
//
//  }

//  private String hopify(String ui, String param, String appId, String source) {
//
//    //remove the link to the full cluster information in the yarn ui
//    ui = ui.replaceAll(
//        "<div id=\"user\">[\\s\\S]+Logged in as: dr.who[\\s\\S]+<div id=\"logo\">",
//        "<div id=\"logo\">");
//    ui = ui.replaceAll(
//        "<tfoot>[\\s\\S]+</tfoot>",
//        "");
//    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td class=\"content\">",
//        "<td class=\"content\">");
//    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td ", "<td ");
//    ui = ui.replaceAll(
//        "<li><a ui-sref=\"submit\"[\\s\\S]+new Job</a></li>", "");
//
//    ui = ui.replaceAll("(?<=(href|src)=.[^>]{0,200})\\?", "@hwqm");
//
//    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-zA-Z])",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/"
//        + source + "/");
//    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-zA-Z])",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/"
//        + source + "/");
//    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\')(?=http)",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-zA-Z])",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/" + param);
//    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/" + param);
//    ui = ui.replaceAll("(?<=\"(stdout\"|stderr\") : \")(?=[a-zA-Z])",
//        "/hopsworks-api/api/project/"
//        + project.getId() + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("here</a>\\s+for full log", "here</a> for latest " + settings.getSparkUILogsOffset()
//        + " bytes of logs");
//    ui = ui.replaceAll("/@hwqmstart=0", "/@hwqmstart=-" + settings.getSparkUILogsOffset());
//    return ui;
//  }

//  private boolean hasAppAccessRight(String trackingUrl) {
//    String appId = "";
//    if (trackingUrl.contains("application_")) {
//      for (String elem : trackingUrl.split("/")) {
//        if (elem.contains("application_")) {
//          appId = elem;
//          break;
//        }
//      }
//    } else if (trackingUrl.contains("container_")) {
//      appId = "application_";
//      for (String elem : trackingUrl.split("/")) {
//        if (elem.contains("container_")) {
//          String[] containerIdElem = elem.split("_");
//          appId = appId + containerIdElem[2] + "_" + containerIdElem[3];
//          break;
//        }
//      }
//    } else if (trackingUrl.contains("appattempt_")) {
//      appId = "application_";
//      for (String elem : trackingUrl.split("/")) {
//        if (elem.contains("appattempt_")) {
//          String[] containerIdElem = elem.split("_");
//          appId = appId + containerIdElem[1] + "_" + containerIdElem[2];
//          break;
//        }
//      }
//    } else {
//      if (trackingUrl.contains("static")) {
//        return true;
//      }
//      return false;
//    }
//    if (!appId.isEmpty()) {
//      String appUser = yarnApplicationstateFacade.findByAppId(appId).
//          getAppuser();
//      if (!project.getName().equals(hdfsUsersBean.getProjectName(
//          appUser))) {
//        return false;
//      }
//    }
//    return true;
//  }

  


//  private void readLog(Execution e, String type, DistributedFileSystemOps dfso, JsonObjectBuilder arrayObjectBuilder)
//      throws IOException {
//    String message;
//    String stdPath;
//    String path = (type.equals("log") ? e.getStdoutPath() : e.getStderrPath());
//    String retry = (type.equals("log") ? "retriableOut" : "retriableErr");
//    boolean status = (type.equals("log") ? e.getFinalStatus().equals(JobFinalStatus.SUCCEEDED) : true);
//    String hdfsPath = "hdfs://" + path;
//    if (path != null && !path.isEmpty() && dfso.exists(hdfsPath)) {
//      if (dfso.listStatus(new org.apache.hadoop.fs.Path(hdfsPath))[0].getLen() > settings.getJobLogsDisplaySize()) {
//        stdPath = path.split(this.project.getName())[1];
//        int fileIndex = stdPath.lastIndexOf("/");
//        String stdDirPath = stdPath.substring(0, fileIndex);
//        arrayObjectBuilder.add(type, "Log is too big to display. Please retrieve it by clicking ");
//        arrayObjectBuilder.add(type + "Path", "/project/" + this.project.getId() + "/datasets" + stdDirPath);
//      } else {
//        try (InputStream input = dfso.open(hdfsPath)) {
//          message = IOUtils.toString(input, "UTF-8");
//        }
//        arrayObjectBuilder.add(type, message.isEmpty() ? "No information." : message);
//        if (message.isEmpty() && e.getState().isFinalState() && e.getAppId() != null && status) {
//          arrayObjectBuilder.add(retry, "true");
//        }
//      }
//    } else {
//      arrayObjectBuilder.add(type, "No log available");
//      if (e.getState().isFinalState() && e.getAppId() != null && status) {
//        arrayObjectBuilder.add(retry, "true");
//      }
//    }
//  }

//  @GET
//  @Path("/getLog/{appId}/{type}")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response getLog(@PathParam("appId") String appId, @PathParam("type") String type) throws JobException {
//    if (Strings.isNullOrEmpty(appId)) {
//      throw new IllegalArgumentException("appId cannot be null or empty.");
//    }
//    Execution execution = exeFacade.findByAppId(appId);
//    if (execution == null) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, Level.FINE, "AppId: " + appId);
//    }
//    if (!execution.getState().isFinalState()) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE, "Job still running.");
//    }
//    if (!execution.getJob().getProject().equals(this.project)) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_ACCESS_ERROR, Level.FINE,
//        "Requested execution does not belong to a job of project: " + project.getName());
//    }
//
//    JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
//    DistributedFileSystemOps dfso = null;
//    try {
//      dfso = dfs.getDfsOps();
//      readLog(execution, type, dfso, arrayObjectBuilder);
//
//    } catch (IOException ex) {
//      Logger.getLogger(JobService.class
//          .getName()).log(Level.SEVERE, null, ex);
//    } finally {
//      if (dfso != null) {
//        dfso.close();
//      }
//    }
//
//    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
//        arrayObjectBuilder.build()).build();
//  }



//  @GET
//  @Path("/retryLogAggregation/{appId}/{type}")
//  @Produces(MediaType.APPLICATION_JSON)
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response retryLogAggregation(@PathParam("appId") String appId,
//      @PathParam("type") String type,
//      @Context HttpServletRequest req) throws JobException {
//    if (appId == null || appId.isEmpty()) {
//      throw new IllegalArgumentException("get log. No ApplicationId.");
//    }
//    Execution execution = exeFacade.findByAppId(appId);
//    if (execution == null) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, Level.FINE, "AppId " + appId);
//    }
//    if (!execution.getState().isFinalState()) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE, "Job still running.");
//    }
//    if (!execution.getJob().getProject().equals(this.project)) {
//      throw new JobException(RESTCodes.JobErrorCode.JOB_ACCESS_ERROR, Level.FINE,
//        "Requested execution does not belong to a job of project: " + project.getName());
//    }
//
//    DistributedFileSystemOps dfso = null;
//    DistributedFileSystemOps udfso = null;
//    Users user = execution.getUser();
//    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
//    String aggregatedLogPath = settings.getAggregatedLogPath(hdfsUser, appId);
//    if (aggregatedLogPath == null) {
//      throw new JobException(RESTCodes.JobErrorCode.LOG_AGGREGATION_NOT_ENABLED, Level.WARNING);
//    }
//    try {
//      dfso = dfs.getDfsOps();
//      udfso = dfs.getDfsOps(hdfsUser);
//      if (!dfso.exists(aggregatedLogPath)) {
//        throw new JobException(RESTCodes.JobErrorCode.LOG_RETRIEVAL_ERROR, Level.WARNING,
//          "This could be caused by the retention policy");
//      }
//      if (type.equals("out")) {
//        String hdfsLogPath = "hdfs://" + execution.getStdoutPath();
//        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
//            isEmpty()) {
//          if (dfso.exists(hdfsLogPath) && dfso.getFileStatus(
//              new org.apache.hadoop.fs.Path(hdfsLogPath)).getLen() > 0) {
//            throw new JobException(RESTCodes.JobErrorCode.LOG_RETRIEVAL_ERROR, Level.WARNING,
//              "Destination file is not empty:" + hdfsLogPath);
//          } else {
//            String[] desiredLogTypes = {"out"};
//            YarnClientWrapper yarnClientWrapper = ycs
//                .getYarnClientSuper(settings.getConfiguration());
//
//            ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
//            YarnMonitor monitor = new YarnMonitor(applicationId,
//                yarnClientWrapper, ycs);
//            try {
//              YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
//                  hdfsLogPath, desiredLogTypes, monitor);
//            } catch (IOException | InterruptedException | YarnException ex) {
//              throw new JobException(RESTCodes.JobErrorCode.LOG_RETRIEVAL_ERROR, Level.SEVERE,
//                "Something went wrong during the log aggregation", ex.getMessage(), ex);
//            } finally {
//              monitor.close();
//            }
//          }
//        }
//      } else if (type.equals("err")) {
//        String hdfsErrPath = "hdfs://" + execution.getStderrPath();
//        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
//            isEmpty()) {
//          if (dfso.exists(hdfsErrPath) && dfso.getFileStatus(
//              new org.apache.hadoop.fs.Path(hdfsErrPath)).getLen() > 0) {
//            throw new JobException(RESTCodes.JobErrorCode.LOG_RETRIEVAL_ERROR, Level.WARNING,
//              "Destination file is not empty:" + hdfsErrPath);
//          } else {
//            String[] desiredLogTypes = {"err", ".log"};
//            YarnClientWrapper yarnClientWrapper = ycs
//                .getYarnClientSuper(settings.getConfiguration());
//            ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
//            YarnMonitor monitor = new YarnMonitor(applicationId,
//                yarnClientWrapper, ycs);
//            try {
//              YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
//                  hdfsErrPath, desiredLogTypes, monitor);
//            } catch (IOException | InterruptedException | YarnException ex) {
//              throw new JobException(RESTCodes.JobErrorCode.LOG_RETRIEVAL_ERROR, Level.SEVERE,
//                "Something went wrong during the log aggregation", ex.getMessage(), ex);
//            } finally {
//              monitor.close();
//            }
//          }
//        }
//      }
//    } catch (IOException ex) {
//      LOGGER.log(Level.SEVERE, null, ex);
//    } finally {
//      if (dfso != null) {
//        dfso.close();
//      }
//      if (udfso != null) {
//        dfs.closeDfsClient(udfso);
//      }
//    }
//    RESTApiJsonResponse json = new RESTApiJsonResponse();
//    json.setSuccessMessage("Log retrieved successfuly.");
//    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
//  }



}
