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
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.description.AppIdDTO;
import io.hops.hopsworks.common.dao.jobs.description.AppInfoDTO;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jobs.yarn.YarnMonitor;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

/**
 *
 * <p>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobService {

  private static final Logger LOGGER = Logger.getLogger(JobService.class.
      getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private ExecutionFacade exeFacade;
  @Inject
  private ExecutionService executions;
  @Inject
  private SparkService spark;
  @Inject
  private AdamService adam;
  @Inject
  private FlinkService flink;
  @Inject
  private TensorFlowService tensorflow;
  @Inject
  private InfluxDBService influxdb;
  @EJB
  private JobController jobController;
  @EJB
  private YarnApplicationAttemptStateFacade appAttemptStateFacade;
  @EJB
  private YarnApplicationstateFacade appStateFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private YarnClientService ycs;
  @EJB
  private Settings settings;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ElasticController elasticController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private YarnApplicationstateFacade appStateBean;

  // No @EJB annotation for Project, it's injected explicitly in ProjectService.
  private Project project;
  private static final String PROXY_USER_COOKIE_NAME = "proxy-user";

  public JobService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project.
   * <p>
   * @param sc
   * @param req
   * @return A list of all defined Jobs in this project.
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findAllJobs(@Context SecurityContext sc,
      @Context HttpServletRequest req)
      throws AppException {
    List<Jobs> jobs = jobFacade.findForProject(project);
    GenericEntity<List<Jobs>> jobList
        = new GenericEntity<List<Jobs>>(jobs) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        jobList).build();
  }

  /**
   * Get the job with the given id in the current project.
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getJob(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(job).build();
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
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/config")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getJobConfiguration(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(job.getJobConfig()).build();
    }
  }

  /**
   * Get the appId for the specified job
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/appId")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAppId(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      Execution execution = exeFacade.findForJob(job).get(0);
      if (execution == null) {
        LOGGER.log(Level.SEVERE, "No job execution found for job {}", job.
            getName());
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      Execution updatedExecution = exeFacade.getExecution(execution.getJob().
          getId());
      if (updatedExecution != null) {
        execution = updatedExecution;
      }

      try {

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(execution.getAppId()).build();

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
            getLocalizedMessage(), e);
      }
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Get all the appIds for the specified job
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{jobId}/appIds")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAppIds(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      List<Execution> executions = exeFacade.findForJob(job);
      if (executions == null || executions.isEmpty()) {
        LOGGER.log(Level.SEVERE, "No job execution found for job {}", job.
            getName());
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      try {
        List<AppIdDTO> appIdStrings = new ArrayList<>();
        for (Execution ex : executions) {
          appIdStrings.add(new AppIdDTO(ex.getAppId()));
        }

        GenericEntity<List<AppIdDTO>> appIds
            = new GenericEntity<List<AppIdDTO>>(appIdStrings) { };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(appIds).build();

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
            getLocalizedMessage(), e);
      }
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    if (project.getId() == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    return hdfsUsername;
  }

  /**
   * Get the projectName for the specified projectId
   * <p>
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/projectName")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getProjectName(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    try {

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(project.getName()).build();

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
          getLocalizedMessage(), e);
    }
    return noCacheResponse.
        getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();

  }

  private List<YarnAppUrlsDTO> getTensorBoardUrls(String hdfsUser, String appId) throws AppException {
    List<YarnAppUrlsDTO> urls = new ArrayList<>();

    DistributedFileSystemOps client = null;

    try {
      client = dfs.getDfsOps(hdfsUser);
      FileStatus[] statuses = client.getFilesystem().globStatus(new org.apache.hadoop.fs.Path("/Projects/" + project.
          getName() + "/Logs/TensorFlow/" + appId + "/TensorBoard.task*"));
      DistributedFileSystem fs = client.getFilesystem();
      for (FileStatus status : statuses) {
        //LOGGER.log(Level.INFO, "Reading tensorboard for: {0}", status.getPath());
        FSDataInputStream in = null;
        try {
          in = fs.open(new org.apache.hadoop.fs.Path(status.getPath().toString()));
          String url = IOUtils.toString(in, "UTF-8");
          int prefix = url.indexOf("http://");
          if (prefix != -1) {
            url = url.substring("http://".length());
          }
          String name = status.getPath().getName();
          urls.add(new YarnAppUrlsDTO(name, url));
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Problem reading file with tensorboard address from HDFS: " + e.getMessage());
        } finally {
          org.apache.hadoop.io.IOUtils.closeStream(in);
        }

      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
          getLocalizedMessage(), e);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
          "Error getting the Tensorboard(s) for this application.");
    } finally {
      if (client != null) {
        dfs.closeDfsClient(client);
      }
    }

    return urls;
  }


  /**
   * Get the Job UI url for the specified job
   * <p>
   * @param appId
   * @param isLivy
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{appId}/ui/{isLivy}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getJobUI(@PathParam("appId") String appId,
      @PathParam("isLivy") String isLivy,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Response noAccess = checkAccessRight(appId);
    if (noAccess != null) {
      return noAccess;
    }
    Response.Status response = Response.Status.OK;
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    String hdfsUser = getHdfsUser(sc);

    try {
      String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(appId);
      if (trackingUrl != null && !trackingUrl.isEmpty()) {
        trackingUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
            + appId + "/prox/" + trackingUrl;
        urls.add(new YarnAppUrlsDTO("spark", trackingUrl));
      }

      if (isLivy.compareToIgnoreCase("true") == 0) {
        YarnApplicationstate appStates;
        appStates = appStateBean.findByAppId(appId);
        if (appStates != null && appStates.getAppname().toUpperCase().contains("TENSORFLOW")) {
          urls.addAll(getTensorBoardUrls(hdfsUser, appId));
        }
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
          getLocalizedMessage(), e);
    }

    GenericEntity<List<YarnAppUrlsDTO>> listUrls = new GenericEntity<List<YarnAppUrlsDTO>>(urls) { };

    return noCacheResponse.getNoCacheResponseBuilder(response)
        .entity(listUrls).build();
  }

  private Response checkAccessRight(String appId) {
    YarnApplicationstate appState = appStateFacade.findByAppId(appId);

    if (appState == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!hdfsUsersBean.getProjectName(appState.getAppuser()).equals(project.getName())) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return null;
    }
  }

  /**
   * Get the Yarn UI url for the specified job
   * <p>
   * @param appId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{appId}/yarnui")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getYarnUI(@PathParam("appId") String appId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Response response = checkAccessRight(appId);
    if (response != null) {
      return response;
    } else {

      try {
        String yarnUrl = "/hopsworks-api/api/project/" + project.getId() + "/jobs/"
            + appId + "/prox/" + settings.getYarnWebUIAddress()
            + "/cluster/app/"
            + appId;

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(yarnUrl).build();

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
            getLocalizedMessage(), e);
      }
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  }

  /**
   * Get application run info for the specified job
   * <p>
   * @param appId
   * @param sc
   * @param req
   * @return url
   * @throws AppException
   */
  @GET
  @Path("/{appId}/appinfo")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAppInfo(@PathParam("appId") String appId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Response response = checkAccessRight(appId);
    if (response != null) {
      return response;
    } else {
      Execution execution = exeFacade.findByAppId(appId);
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
        int index=0;
        if (queryResult != null && queryResult.getResults() != null){
          for(QueryResult.Result res: queryResult.getResults()){
            if(res.getSeries()!=null){
              for(QueryResult.Series series : res.getSeries()){
                List<List<Object>> values = series.getValues();
                if(values!=null){
                  nbExecutors += values.size();
                  for(List<Object> l: values){
                    executorInfo.put(index,Stream.of(Objects.toString(l.get(1))).collect(Collectors.toList()));
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

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(appInfo).build();

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
            getLocalizedMessage(), e);
      }
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
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
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{appId}/prox/{path: .+}")
  @Produces(MediaType.WILDCARD)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getProxy(@PathParam("appId")
      final String appId,
      @PathParam("path")
      final String param,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    Response response = checkAccessRight(appId);
    if (response != null) {
      return response;
    }
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
      HostConfiguration config = new HostConfiguration();
      InetAddress localAddress = InetAddress.getLocalHost();
      config.setLocalAddress(localAddress);

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

      client.executeMethod(config, method);
      Response.ResponseBuilder responseBuilder = noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.OK);
      for (Header header : method.getResponseHeaders()) {
        responseBuilder.header(header.getName(), header.getValue());
      }
      if (method.getResponseHeader("Content-Type") == null || method.
          getResponseHeader("Content-Type").getValue().contains("html")) {
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
      LOGGER.log(Level.SEVERE, "exception while geting job ui " + e.
          getLocalizedMessage(), e);
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
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

  @GET
  @Path("/template/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getConfigurationTemplate(@PathParam("type") String type,
      @Context SecurityContext sc, @Context HttpServletRequest req) {
    JobConfiguration template = JobConfiguration.JobConfigurationFactory.
        getJobConfigurationTemplate(JobType.valueOf(type));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(template).build();
  }

  /**
   * Get all the jobs in this project that have a running execution. The return
   * value is a JSON object, where each job
   * id is a key and the corresponding boolean indicates whether the job is
   * running or not.
   * <p/>
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/running")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getConfigurationTemplate(@Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<Jobs> running = jobFacade.getRunningJobs(project);
    List<Jobs> allJobs = jobFacade.findForProject(project);
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (Jobs desc : allJobs) {
      try {
        List<Execution> jobExecutions = exeFacade.findForJob(desc);
        if (jobExecutions != null && jobExecutions.isEmpty() == false) {
          Execution execution = jobExecutions.get(0);
          builder.add(desc.getId().toString(), Json.createObjectBuilder()
              .add("running", false)
              .add("state", execution.getState().toString())
              .add("finalStatus", execution.getFinalStatus().toString())
              .add("progress", execution.getProgress())
              .add("duration", execution.getExecutionDuration())
              .add("submissiontime", execution.getSubmissionTime().
                  toString())
          );
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        LOGGER.log(Level.WARNING, "No execution was found: {0}", e
            .getMessage());
      }
    }
    for (Jobs desc : running) {
      try {
        Execution execution = exeFacade.findForJob(desc).get(0);
        Execution updatedExecution = exeFacade.getExecution(execution.getJob().
            getId());
        if (updatedExecution != null) {
          execution = updatedExecution;
        }
        long executiontime = System.currentTimeMillis() - execution.
            getSubmissionTime().getTime();
        //not given appId (not submited yet)
        if (execution.getAppId() == null && executiontime > 60000l * 5) {
          exeFacade.updateState(execution, JobState.INITIALIZATION_FAILED);
          exeFacade.updateFinalStatus(execution, JobFinalStatus.FAILED);
          continue;
        }

        String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(
            execution.getAppId());
        builder.add(desc.getId().toString(),
            Json.createObjectBuilder()
                .add("running", true)
                .add("state", execution.getState().toString())
                .add("finalStatus", execution.getFinalStatus().
                    toString())
                .add("progress", execution.getProgress())
                .add("duration", execution.getExecutionDuration())
                .add("submissiontime", execution.getSubmissionTime().
                    toString())
                .add("url", trackingUrl)
        );
      } catch (ArrayIndexOutOfBoundsException e) {
        LOGGER.log(Level.WARNING, "No execution was found: {0}", e
            .getMessage());
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(builder.build()).build();
  }

  /**
   * Get the log information related to a job. The return value is a JSON
   * object, with format logset=[{"time":"JOB
   * EXECUTION TIME"}, {"log":"INFORMATION LOG"}, {"err":"ERROR LOG"}]
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/{jobId}/showlog")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getLogInformation(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) {

    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    List<Execution> executionHistory = exeFacade.findbyProjectAndJobId(project, jobId);
    JsonObjectBuilder arrayObjectBuilder;
    if (executionHistory != null && !executionHistory.isEmpty()) {
      for (Execution e : executionHistory) {
        arrayObjectBuilder = Json.createObjectBuilder();
        arrayObjectBuilder.add("jobId", e.getJob().getId());
        arrayObjectBuilder.add("appId", e.getAppId() == null ? "" : e.getAppId());
        arrayObjectBuilder.add("time", e.getSubmissionTime().toString());
        arrayBuilder.add(arrayObjectBuilder);
      }
    } else {
      arrayObjectBuilder = Json.createObjectBuilder();
      arrayObjectBuilder.add("jobId", "");
      arrayObjectBuilder.add("appId", "");
      arrayObjectBuilder.add("time", "Not available");
      arrayObjectBuilder.add("log", "No log available");
      arrayObjectBuilder.add("err", "No log available");
      arrayBuilder.add(arrayObjectBuilder);
    }
    builder.add("logset", arrayBuilder);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(builder.build()).build();
  }

  private void readLog(Execution e, String type, DistributedFileSystemOps dfso, JsonObjectBuilder arrayObjectBuilder)
      throws IOException {
    String message;
    String stdPath;
    String path = (type.equals("log") ? e.getStdoutPath() : e.getStderrPath());
    String retry = (type.equals("log") ? "retriableOut" : "retriableErr");
    boolean status = (type.equals("log") ? e.getFinalStatus().equals(JobFinalStatus.SUCCEEDED) : true);
    String hdfsPath = "hdfs://" + path;
    if (path != null && !path.isEmpty() && dfso.exists(hdfsPath)) {
      if (dfso.listStatus(new org.apache.hadoop.fs.Path(hdfsPath))[0].getLen() > settings.getJobLogsDisplaySize()) {
        stdPath = path.split(this.project.getName())[1];
        int fileIndex = stdPath.lastIndexOf("/");
        String stdDirPath = stdPath.substring(0, fileIndex);
        arrayObjectBuilder.add(type, "Log is too big to display. Please retrieve it by clicking ");
        arrayObjectBuilder.add(type + "Path", "/project/" + this.project.getId() + "/datasets" + stdDirPath);
      } else {
        try (InputStream input = dfso.open(hdfsPath)) {
          message = IOUtils.toString(input, "UTF-8");
        }
        arrayObjectBuilder.add(type, message.isEmpty() ? "No information." : message);
        if (message.isEmpty() && e.getState().isFinalState() && e.getAppId() != null && status) {
          arrayObjectBuilder.add(retry, "true");
        }
      }
    } else {
      arrayObjectBuilder.add(type, "No log available");
      if (e.getState().isFinalState() && e.getAppId() != null && status) {
        arrayObjectBuilder.add(retry, "true");
      }
    }
  }

  @GET
  @Path("/getLog/{appId}/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getLog(@PathParam("appId") String appId,
      @PathParam("type") String type) throws AppException {
    if (appId == null || appId.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "Can not get log. No ApplicationId.");
    }
    Execution execution = exeFacade.findByAppId(appId);
    if (execution == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "No excution for appId " + appId);
    }
    if (!execution.getState().isFinalState()) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "Job still running.");
    }
    if (!execution.getJob().getProject().equals(this.project)) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "No excution for appId " + appId
          + ".");
    }

    JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      readLog(execution, type, dfso, arrayObjectBuilder);

    } catch (IOException ex) {
      Logger.getLogger(JobService.class
          .getName()).log(Level.SEVERE, null, ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        arrayObjectBuilder.build()).build();
  }

  @GET
  @Path("/getLogByJobId/{jobId}/{submissionTime}/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getLogByJobId(@PathParam("jobId") Integer jobId, @PathParam("submissionTime") String submissionTime,
      @PathParam("type") String type) throws AppException {
    if (jobId == null || jobId <= 0) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Can not get log. No JobId.");
    }
    if (submissionTime == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Can not get log. With no submission time.");
    }
    Jobs job = jobFacade.find(jobId);
    if (job == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Can not get log. Job not found.");
    }
    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
    Date date;
    try {
      date = sdf.parse(submissionTime);
    } catch (ParseException ex) {
      LOGGER.log(Level.SEVERE, "Can not get log. Incorrect submission time. ", ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Can't get log. Incorrect submission time.");
    }
    Execution execution = exeFacade.findByJobIdAndSubmissionTime(date, job);
    if (execution == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "No excution for jobId " + jobId);
    }
    if (!execution.getState().isFinalState()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Job still running.");
    }
    if (!execution.getJob().getProject().equals(this.project)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "No excution for jobId " + jobId + ".");
    }

    JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      readLog(execution, type, dfso, arrayObjectBuilder);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(arrayObjectBuilder.build()).build();
  }

  @GET
  @Path("/retryLogAggregation/{appId}/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response retryLogAggregation(@PathParam("appId") String appId,
      @PathParam("type") String type,
      @Context HttpServletRequest req) throws AppException {
    if (appId == null || appId.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "Can not get log. No ApplicationId.");
    }
    Execution execution = exeFacade.findByAppId(appId);
    if (execution == null) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "No excution for appId " + appId);
    }
    if (!execution.getState().isFinalState()) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "Job still running.");
    }
    if (!execution.getJob().getProject().equals(this.project)) {
      throw new AppException(Response.Status.BAD_REQUEST.
          getStatusCode(), "No excution for appId " + appId
          + ".");
    }

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    Users user = execution.getUser();
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    String aggregatedLogPath = settings.getAggregatedLogPath(hdfsUser, appId);
    if (aggregatedLogPath == null) {
      throw new AppException(Response.Status.NOT_FOUND.
          getStatusCode(),
          "Aggregation is not enabled.");
    }
    try {
      dfso = dfs.getDfsOps();
      udfso = dfs.getDfsOps(hdfsUser);
      if (!dfso.exists(aggregatedLogPath)) {
        throw new AppException(Response.Status.NOT_FOUND.
            getStatusCode(),
            "Logs not available. This could be caused by the rentention policy");
      }
      if (type.equals("out")) {
        String hdfsLogPath = "hdfs://" + execution.getStdoutPath();
        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
            isEmpty()) {
          if (dfso.exists(hdfsLogPath) && dfso.getFileStatus(
              new org.apache.hadoop.fs.Path(hdfsLogPath)).getLen() > 0) {
            throw new AppException(Response.Status.BAD_REQUEST.
                getStatusCode(),
                "Destination file is not empty.");
          } else {
            String[] desiredLogTypes = {"out"};
            YarnClientWrapper yarnClientWrapper = ycs
                .getYarnClientSuper(settings.getConfiguration());

            ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
            YarnMonitor monitor = new YarnMonitor(applicationId,
                yarnClientWrapper, ycs);
            try {
              YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
                  hdfsLogPath, desiredLogTypes, monitor);
            } catch (IOException | InterruptedException | YarnException ex) {
              throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "something went wrong during the log aggregation");
            } finally {
              monitor.close();
            }
          }
        }
      } else if (type.equals("err")) {
        String hdfsErrPath = "hdfs://" + execution.getStderrPath();
        if (execution.getStdoutPath() != null && !execution.getStdoutPath().
            isEmpty()) {
          if (dfso.exists(hdfsErrPath) && dfso.getFileStatus(
              new org.apache.hadoop.fs.Path(hdfsErrPath)).getLen() > 0) {
            throw new AppException(Response.Status.BAD_REQUEST.
                getStatusCode(),
                "Destination file is not empty.");
          } else {
            String[] desiredLogTypes = {"err", ".log"};
            YarnClientWrapper yarnClientWrapper = ycs
                .getYarnClientSuper(settings.getConfiguration());
            ApplicationId applicationId = ConverterUtils.toApplicationId(appId);
            YarnMonitor monitor = new YarnMonitor(applicationId,
                yarnClientWrapper, ycs);
            try {
              YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath,
                  hdfsErrPath, desiredLogTypes, monitor);
            } catch (IOException | InterruptedException | YarnException ex) {
              throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "something went wrong during the log aggregation");
            } finally {
              monitor.close();
            }
          }
        }
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Log retrieved successfuly.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }

  /**
   * Delete the job associated to the project and jobid. The return value is a
   * JSON object stating operation successful
   * or not.
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @DELETE
  @Path("/{jobId}/deleteJob")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteJob(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    LOGGER.log(Level.INFO, "Request to delete job");
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    } else {
      try {
        LOGGER.log(Level.INFO, "Request to delete job name ={0} job id ={1}",
            new Object[]{job.getName(), job.getId()});

        elasticController.deleteJobLogs(project.getName(), "logs", settings.getJobLogsIdField(), job.getId());
        jobFacade.removeJob(job);
        LOGGER.log(Level.INFO, "Deleted job name ={0} job id ={1}",
            new Object[]{job.getName(), job.getId()});
        JsonResponse json = new JsonResponse();
        json.setSuccessMessage("Deleted job " + job.getName() + " successfully");
        activityFacade.persistActivity(ActivityFacade.DELETED_JOB + job.
            getName(), project, sc.getUserPrincipal().getName());
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(json).build();
      } catch (DatabaseException ex) {
        LOGGER.log(Level.WARNING,
            "Job cannot be deleted  job name ={0} job id ={1}",
            new Object[]{job.getName(), job.getId()});
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ex.getMessage());
      }
    }
  }

  /**
   * Get the ExecutionService for the job with given id.
   * <p>
   * @param jobId
   * @return
   */
  @Path(
      "/{jobId}/executions")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public ExecutionService executions(@PathParam("jobId") int jobId) {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return null;
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return null;
    } else {
      return this.executions.setJob(job);
    }
  }

  @POST
  @Path("/updateschedule/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response updateSchedule(ScheduleDTO schedule,
      @PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Jobs job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,
          "A user is trying to access a job outside their project!");
      return noCacheResponse.
          getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    } else {
      try {
        boolean isScheduleUpdated = jobFacade.updateJobSchedule(jobId, schedule);
        if (isScheduleUpdated) {
          boolean status = jobController.scheduleJob(jobId);
          if (status) {
            JsonResponse json = new JsonResponse();
            json.setSuccessMessage("Scheduled job " + job.getName()
                + " successfully");
            activityFacade.persistActivity(ActivityFacade.SCHEDULED_JOB + job.
                getName(), project, sc.getUserPrincipal().getName());
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
          } else {
            LOGGER.log(Level.WARNING,
                "Schedule is not created in the scheduler for the jobid {0}",
                jobId);
          }
        } else {
          LOGGER.log(Level.WARNING,
              "Schedule is not updated in DB for the jobid {0}", jobId);
        }

      } catch (DatabaseException ex) {
        LOGGER.log(Level.WARNING, "Cannot update schedule {0}", ex.getMessage());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ex.getMessage());
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.INTERNAL_SERVER_ERROR).build();
  }

  @Path(
      "/spark")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public SparkService spark() {
    return this.spark.setProject(project);
  }

  @Path("/pyspark")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public SparkService pyspark() {
    return this.spark.setProject(project);
  }

  @Path("/tfspark")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public SparkService tfspark() {
    return this.spark.setProject(project);
  }

  @Path("/tensorflow")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public TensorFlowService tensorflow() {
    return this.tensorflow.setProject(project);
  }

  @Path("/adam")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public AdamService adam() {
    return this.adam.setProject(project);
  }

  @Path("/flink")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public FlinkService flink() {
    return this.flink.setProject(project);
  }

  @Path("/{appId}/influxdb")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public InfluxDBService influxdb(@PathParam("appId") String appId) {
    return this.influxdb.setAppId(appId);
  }
}
