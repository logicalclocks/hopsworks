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

package io.hops.hopsworks.common.jobs.execution;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.JobsHistoryFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.AppInfoDTO;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jobs.yarn.YarnMonitor;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Takes care of booting the execution of a job.
 */
@Stateless
public class ExecutionController {

  //Controllers
  @EJB
  private SparkController sparkController;
  @EJB
  private FlinkController flinkController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobsHistoryFacade jobHistoryFac;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade execFacade;
  @EJB
  private YarnClientService ycs;
  @EJB
  private YarnApplicationAttemptStateFacade appAttemptStateFacade;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  private static final Logger LOGGER = Logger.getLogger(ExecutionController.class.getName());
  private static final String REMOTE_PROTOCOL = "hdfs://";
  private static final String PROXY_USER_COOKIE_NAME = "proxy-user";
  
  private static final HashSet<String> PASS_THROUGH_HEADERS = new HashSet<>(
    Arrays.asList("User-Agent", "user-agent", "Accept", "accept",
        "Accept-Encoding", "accept-encoding",
        "Accept-Language",
        "accept-language",
        "Accept-Charset", "accept-charset"));
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Execution start(Jobs job, Users user) throws GenericException, JobException, ServiceException {
    //A job can only have one execution running
    List<Execution> jobExecs = execFacade.findByJob(job);
    if(!jobExecs.isEmpty()) {
      //Sort descending based on executionId
      jobExecs.sort((lhs, rhs) -> {
        return lhs.getId() > rhs.getId() ? -1 : (lhs.getId() < rhs.getId()) ? 1 : 0;
      });
      if(!jobExecs.get(0).getState().isFinalState()){
        throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE,
          "Cannot start an execution while another one for the same job has not finished.");
      }
    }
    
    Execution exec;
    switch (job.getJobType()) {
      case FLINK:
        return flinkController.startJob(job, user, null);
      case SPARK:
        exec = sparkController.startJob(job, user);
        if (exec == null) {
          throw new IllegalArgumentException("Problem getting execution object for: " + job.getJobType());
        }
        int execId = exec.getId();
        SparkJobConfiguration config = (SparkJobConfiguration) job.getJobConfig();
        String path = config.getAppPath();
        String patternString = REMOTE_PROTOCOL + "(.*)\\s";
        Pattern.compile(patternString).matcher(path);
        String[] parts = path.split("/");
        String pathOfInode = path.replace(REMOTE_PROTOCOL + parts[2], "");
        Inode inode = inodes.getInodeAtPath(pathOfInode);
        String inodeName = inode.getInodePK().getName();

        jobHistoryFac.persist(user, job, execId, exec.getAppId());
        activityFacade.persistActivity(ActivityFacade.EXECUTED_JOB + inodeName, job.getProject(), user, ActivityFacade.
            ActivityFlag.JOB);
        break;
      case PYSPARK:
        exec = sparkController.startJob(job, user);
        if (exec == null) {
          throw new IllegalArgumentException("Error while getting execution object for: " + job.getJobType());
        }
        break;
      default:
        throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ACTION, Level.FINE, "Unsupported job type: "
          + job.getJobType());
    }

    return exec;
  }
  
  public Execution kill(Jobs job, Users user) throws JobException {
    //Get the last appId for the job, a job cannot have two concurrent applications running.
    List<Execution> jobExecs = execFacade.findByJob(job);
    if(!jobExecs.isEmpty()) {
      //Sort descending based on executionId
      jobExecs.sort((lhs, rhs) -> lhs.getId() > rhs.getId() ? -1 : (lhs.getId() < rhs.getId()) ? 1 : 0);
      String appId = jobExecs.get(0).getAppId();
      //Look for unique marker file which means it is a streaming job. Otherwise proceed with normal kill.
      DistributedFileSystemOps udfso = null;
      String username = hdfsUsersController.getHdfsUserName(job.getProject(), user);
      try {
        udfso = dfs.getDfsOps(username);
        String marker = settings.getJobMarkerFile(job, appId);
        if (udfso.exists(marker)) {
          udfso.rm(new org.apache.hadoop.fs.Path(marker), false);
        } else {
  
          YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
          try {
            YarnClient client = yarnClientWrapper.getYarnClient();
            client.killApplication(ApplicationId.fromString(appId));
          } catch (YarnException e) {
            throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, Level.WARNING, e.getMessage(), null, e);
          } finally {
            ycs.closeYarnClient(yarnClientWrapper);
          }
        }
        return execFacade.findByAppId(appId);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Could not remove marker file for job:" + job.getName() + "with appId:" + appId, ex);
      } finally {
        if (udfso != null) {
          dfs.closeDfsClient(udfso);
        }
      }
    }
    return null;
  }
  
  public void stop(Jobs job, Users user, String appid) {
    switch (job.getJobType()) {
      case SPARK:
        sparkController.stopJob(job, user, appid);
        break;
      case FLINK:
        flinkController.stopJob(job, user, appid, null);
        break;
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
    }
  }
  
  
  //====================================================================================================================
  // Execution logs
  //====================================================================================================================
  
  public JobLogDTO getLog(Execution execution, JobLogDTO.LogType type) throws JobException {
    if (!execution.getState().isFinalState()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE, "Job still running.");
    }
    
    JobLogDTO dto = new JobLogDTO(type);
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String message;
      String stdPath;
      String path = (dto.getType() == JobLogDTO.LogType.OUT ? execution.getStdoutPath() : execution.getStderrPath());
      JobLogDTO.Retriable retriable = (dto.getType() == JobLogDTO.LogType.OUT ? JobLogDTO.Retriable.RETRIEABLE_OUT :
        JobLogDTO.Retriable.RETRIABLE_ERR);
      boolean status = (dto.getType() != JobLogDTO.LogType.OUT || execution.getFinalStatus().equals(JobFinalStatus
        .SUCCEEDED));
      String hdfsPath = REMOTE_PROTOCOL + path;
      if (!Strings.isNullOrEmpty(path) && dfso.exists(hdfsPath)) {
        if (dfso.listStatus(new org.apache.hadoop.fs.Path(hdfsPath))[0].getLen() > settings.getJobLogsDisplaySize()) {
          Project project = execution.getJob().getProject();
          stdPath = path.split(project.getName())[1];
          int fileIndex = stdPath.lastIndexOf('/');
          String stdDirPath = stdPath.substring(0, fileIndex);
          dto.setPath("/project/" + project.getId() + "/datasets" + stdDirPath);
        } else {
          try (InputStream input = dfso.open(hdfsPath)) {
            message = IOUtils.toString(input, "UTF-8");
          }
          dto.setLog(message.isEmpty() ? "No information." : message);
          if (message.isEmpty() && execution.getState().isFinalState() && execution.getAppId() != null && status) {
            dto.setRetriable(retriable);
          }
        }
      } else {
        dto.setLog("No log available");
        if (execution.getState().isFinalState() && execution.getAppId() != null && status) {
          dto.setRetriable(retriable);
        }
      }
      
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    return dto;
  }
  
  public JobLogDTO retryLogAggregation(Execution execution, JobLogDTO.LogType type) throws JobException {
    if (!execution.getState().isFinalState()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE, "Job still running.");
    }
    
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    Users user = execution.getUser();
    String hdfsUser = hdfsUsersController.getHdfsUserName(execution.getJob().getProject(), user);
    String aggregatedLogPath = settings.getAggregatedLogPath(hdfsUser, execution.getAppId());
    if (aggregatedLogPath == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_LOG, Level.INFO,"Log aggregation is not enabled");
    }
    try {
      dfso = dfs.getDfsOps();
      udfso = dfs.getDfsOps(hdfsUser);
      if (!dfso.exists(aggregatedLogPath)) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_LOG, Level.WARNING,
          "Logs not available. This could be caused by the retention policy.");
      }
      String hdfsLogPath = null;
      String[] desiredLogTypes = null;
      switch (type){
        case OUT:
          hdfsLogPath = REMOTE_PROTOCOL + execution.getStdoutPath();
          desiredLogTypes = new String[]{type.name()};
          break;
        case ERR:
          hdfsLogPath = REMOTE_PROTOCOL + execution.getStderrPath();
          desiredLogTypes = new String[]{type.name(), ".log"};
          break;
        default:
          break;
      }
      
      if (!Strings.isNullOrEmpty(hdfsLogPath)) {
        YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
        ApplicationId applicationId = ConverterUtils.toApplicationId(execution.getAppId());
        YarnMonitor monitor = new YarnMonitor(applicationId, yarnClientWrapper, ycs);
        try {
          YarnLogUtil.copyAggregatedYarnLogs(udfso, aggregatedLogPath, hdfsLogPath, desiredLogTypes, monitor);
        } catch (IOException | InterruptedException | YarnException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          throw new JobException(RESTCodes.JobErrorCode.JOB_LOG, null, ex.getMessage());
        } finally {
          monitor.close();
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
    
    return getLog(execution, type);
  }
  
  
  //====================================================================================================================
  // Execution Proxies
  //====================================================================================================================
  
  public String getExecutionUI(Execution execution) throws JobException {
    String trackingUrl = appAttemptStateFacade.findTrackingUrlByAppId(execution.getAppId());
    if (trackingUrl != null && !trackingUrl.isEmpty()) {
      return "/project/" + execution.getJob().getProject().getId() + "/jobs/" + execution.getAppId() + "/prox/" +
        trackingUrl;
    }
    throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_TRACKING_URL_NOT_FOUND, Level.FINE,
      "ExecutionId:" + execution.getId());
  }
  
  public String getExecutionYarnUI(int execId) {
    Execution execution = execFacade.findById(execId);
    return "/project/" + execution.getJob().getProject().getId()
      + "/jobs/" + execution.getAppId() + "/prox/" + settings.getYarnWebUIAddress()
      + "/cluster/app/" + execution.getAppId();
  }
  
  public AppInfoDTO getExecutionAppInfo(Execution execution) {
    
    long startTime = System.currentTimeMillis() - 60000;
    long endTime = System.currentTimeMillis();
    boolean running = true;
    if (execution != null) {
      startTime = execution.getSubmissionTime().getTime();
      endTime = startTime + execution.getExecutionDuration();
      running = !execution.getState().isFinalState();
    }
    
    InfluxDB influxDB = null;
    int nbExecutors = 0;
    HashMap<Integer, List<String>> executorInfo;
    try {
      influxDB = InfluxDBFactory.connect(settings.getInfluxDBAddress(),
        settings.getInfluxDBUser(),
        settings.getInfluxDBPW());
      
      // Transform application_1493112123688_0001 to 1493112123688_0001
      // application_ = 12 chars
      String timestamp_attempt = execution.getAppId().substring(12);
      
      Query query = new Query("show tag values from nodemanager with key=\"source\" " + "where source =~ /^.*"
        + timestamp_attempt + ".*$/", "graphite");
      QueryResult queryResult = influxDB.query(query, TimeUnit.MILLISECONDS);
      
      
      executorInfo = new HashMap<>();
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
      String vCoreTemp;
      HashMap<String, String> hostnameVCoreCache = new HashMap<>();
      
      for (Map.Entry<Integer, List<String>> entry : executorInfo.entrySet()) {
        query =
          new Query("select MilliVcoreUsageAvgMilliVcores, hostname from nodemanager where source = \'" + entry.
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
      
    } finally {
      if (influxDB != null) {
        influxDB.close();
        
      }
    }
    
    AppInfoDTO appInfo = new AppInfoDTO(execution.getAppId(), startTime, running, endTime, nbExecutors, executorInfo);
    return appInfo;
  }
  
//  public Response.ResponseBuilder getExecutionProxy(Execution execution, String param, HttpServletRequest req)
//    throws JobException, IOException {
//    String trackingUrl;
//    if (param.matches("http([a-zA-Z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
//      trackingUrl = param;
//    } else {
//      trackingUrl = "http://" + param;
//    }
//    trackingUrl = trackingUrl.replace("@hwqm", "?");
//    if (!hasAppAccessRight(trackingUrl, execution.getJob().getProject().getName())) {
//      LOGGER.log(Level.SEVERE, "A user is trying to access an app outside their project!");
//      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINE);
//    }
//    org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(trackingUrl, false);
//
//    HttpClientParams params = new HttpClientParams();
//    params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
//    params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
//      true);
//    HttpClient client = new HttpClient(params);
//
//    final HttpMethod method = new GetMethod(uri.getEscapedURI());
//    Enumeration<String> names = req.getHeaderNames();
//    while (names.hasMoreElements()) {
//      String name = names.nextElement();
//      String value = req.getHeader(name);
//      if (PASS_THROUGH_HEADERS.contains(name)) {
//        //yarn does not send back the js if encoding is not accepted
//        //but we don't want to accept encoding for the html because we
//        //need to be able to parse it
//        if (!name.toLowerCase().equals("accept-encoding") || trackingUrl.contains(".js")) {
//          method.setRequestHeader(name, value);
//        }
//      }
//    }
//    String user = req.getRemoteUser();
//    if (user != null && !user.isEmpty()) {
//      method.setRequestHeader("Cookie", PROXY_USER_COOKIE_NAME + "=" + URLEncoder.encode(user, "ASCII"));
//    }
//
//    client.executeMethod(method);
//    Response.ResponseBuilder responseBuilder = Response.ok();
//    for (Header header : method.getResponseHeaders()) {
//      responseBuilder.header(header.getName(), header.getValue());
//    }
//    if (method.getResponseHeader("Content-Type") == null || method.
//      getResponseHeader("Content-Type").getValue().contains("html")
//      || method.getPath().contains("/allexecutors")) {
//      final String source = "http://" + method.getURI().getHost() + ":"
//        + method.getURI().getPort();
//      if (method.getResponseHeader("Content-Length") == null) {
//        responseBuilder.entity(new StreamingOutput() {
//          @Override
//          public void write(OutputStream out) throws IOException {
//            Writer writer
//              = new BufferedWriter(new OutputStreamWriter(out));
//            InputStream stream = method.getResponseBodyAsStream();
//            Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
//            char[] buffer = new char[4 * 1024];
//            String remaining = "";
//            int n;
//            while ((n = in.read(buffer)) != -1) {
//              StringBuilder strb = new StringBuilder();
//              strb.append(buffer, 0, n);
//              String s = remaining + strb.toString();
//              remaining = s.substring(s.lastIndexOf('>') + 1, s.length());
//              s = hopify(s.substring(0, s.lastIndexOf('>') + 1), param,
//                execution.getJob().getProject().getId(),
//                execution.getAppId(),
//                source);
//              writer.write(s);
//            }
//            writer.flush();
//          }
//        });
//      } else {
//        String s = hopify(method.getResponseBodyAsString(), param, execution.getJob().getProject().getId(),
//          execution.getAppId(), source);
//        responseBuilder.entity(s);
//        responseBuilder.header("Content-Length", s.length());
//      }
//
//    } else {
//      responseBuilder.entity((StreamingOutput) out -> {
//        InputStream stream = method.getResponseBodyAsStream();
//        org.apache.hadoop.io.IOUtils.copyBytes(stream, out, 4096, true);
//        out.flush();
//      });
//    }
//    return responseBuilder;
//  }
  
  
//  private String hopify(String ui, String param, int projectId, String appId, String source) {
//
//    //remove the link to the full cluster information in the yarn ui
//    ui = ui.replaceAll(
//      "<div id=\"user\">[\\s\\S]+Logged in as: dr.who[\\s\\S]+<div id=\"logo\">",
//      "<div id=\"logo\">");
//    ui = ui.replaceAll(
//      "<tfoot>[\\s\\S]+</tfoot>",
//      "");
//    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td class=\"content\">",
//      "<td class=\"content\">");
//    ui = ui.replaceAll("<td id=\"navcell\">[\\s\\S]+<td ", "<td ");
//    ui = ui.replaceAll(
//      "<li><a ui-sref=\"submit\"[\\s\\S]+new Job</a></li>", "");
//
//    ui = ui.replaceAll("(?<=(href|src)=.[^>]{0,200})\\?", "@hwqm");
//
//    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-zA-Z])",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/"
//        + source + "/");
//    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-zA-Z])",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/"
//        + source + "/");
//    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/api/project/"
//      + projectId + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/api/project/"
//      + projectId + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\')(?=http)",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-zA-Z])",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/" + param);
//    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/" + param);
//    ui = ui.replaceAll("(?<=\"(stdout\"|stderr\") : \")(?=[a-zA-Z])",
//      "/hopsworks-api/api/project/"
//        + projectId + "/jobs/" + appId + "/prox/");
//    ui = ui.replaceAll("here</a>\\s+for full log", "here</a> for latest " + settings.getSparkUILogsOffset()
//      + " bytes of logs");
//    ui = ui.replaceAll("/@hwqmstart=0", "/@hwqmstart=-" + settings.getSparkUILogsOffset());
//    return ui;
//  }
  
//  private boolean hasAppAccessRight(String projectName, String trackingUrl) {
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
//      String appUser = yarnApplicationstateFacade.findByAppId(appId).getAppuser();
//      if (!projectName.equals(hdfsUsersController.getProjectName(appUser))) {
//        return false;
//      }
//    }
//    return true;
//  }
  
  public void checkAccessRight(String appId, Project project) throws JobException {
    YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(appId);
    
    if (appState == null) {
      throw new JobException(RESTCodes.JobErrorCode.APPID_NOT_FOUND, Level.FINE);
    } else if (!hdfsUsersController.getProjectName(appState.getAppuser()).equals(project.getName())) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ACCESS_ERROR, Level.FINE);
    }
  }
  
  
  //====================================================================================================================
  // TensorBoard
  //====================================================================================================================
  
  public List<YarnAppUrlsDTO> getTensorBoardUrls(Users user, Execution execution, Jobs job) throws JobException {
    return getTensorBoardUrls(user, execution.getAppId(), job.getProject());
  }
  
  public List<YarnAppUrlsDTO> getTensorBoardUrls(Users user, String appId, Project project)
    throws JobException {
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    DistributedFileSystemOps udfso = null;
  
    try {
      String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
  
      udfso = dfs.getDfsOps(hdfsUser);
      FileStatus[] statuses = udfso.getFilesystem().globStatus(
        new org.apache.hadoop.fs.Path(
          "/Projects/" + project.getName() + "/Experiments/" + appId + "/TensorBoard.*"));
    
      for (FileStatus status : statuses) {
        LOGGER.log(Level.FINE, "Reading tensorboard for: {0}", status.getPath());
        FSDataInputStream in = null;
        try {
          in = udfso.open(new org.apache.hadoop.fs.Path(status.getPath().toString()));
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
      throw new JobException(RESTCodes.JobErrorCode.TENSORBOARD_ERROR, Level.SEVERE, null, e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  
    return urls;
  }
}
