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
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.AppInfoDTO;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jobs.yarn.YarnMonitor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
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
import java.io.UnsupportedEncodingException;
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
  private InodeController inodeController;
  @EJB
  private ActivityFacade activityFacade;
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
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private AsynchronousJobExecutor async;

  private static final Logger LOGGER = Logger.getLogger(ExecutionController.class.getName());
  private static final String REMOTE_PROTOCOL = "hdfs://";
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Execution start(Jobs job, Users user) throws JobException, GenericException, ServiceException,
    ProjectException, UnsupportedEncodingException {
    //A job can only have one execution running
    List<Execution> jobExecs = execFacade.findByJob(job);
    if(!jobExecs.isEmpty()) {
      //Sort descending based on executionId
      jobExecs.sort((lhs, rhs) -> rhs.getId().compareTo(lhs.getId()));
      if(!jobExecs.get(0).getState().isFinalState()){
        throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_INVALID_STATE, Level.FINE,
          "Cannot start an execution while another one for the same job has not finished.");
      }
    }

    // A user should not be able to start a job if the project is prepaid and it doesn't have quota.
    if(job.getProject().getPaymentType().equals(PaymentType.PREPAID)){
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(job.getProject().getName());
      if(projectQuota == null || projectQuota.getQuotaRemaining() <= 0){
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_ERROR, Level.FINE);
      }
    }

    Execution exec;
    switch (job.getJobType()) {
      case BEAM_FLINK:
      case FLINK:
        //Materialize certs
        return flinkController.startJob(job, user);
      case SPARK:
        exec = sparkController.startJob(job, user);
        if (exec == null) {
          throw new IllegalArgumentException("Problem getting execution object for: " + job.getJobType());
        }
        SparkJobConfiguration config = (SparkJobConfiguration) job.getJobConfig();
        String path = config.getAppPath();
        String pathOfInode = Utils.prepPath(path);
        Inode inode = inodeController.getInodeAtPath(pathOfInode);
        String inodeName = inode.getInodePK().getName();

        activityFacade.persistActivity(ActivityFacade.EXECUTED_JOB + inodeName, job.getProject(), user,
          ActivityFlag.JOB);
        break;
      case PYSPARK:
        if(!job.getProject().getConda()){
          throw new ProjectException(RESTCodes.ProjectErrorCode.ANACONDA_NOT_ENABLED, Level.FINEST);
        }
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
  
  public Execution stop(Jobs job) throws JobException {
    switch (job.getJobType()) {
      case SPARK:
      case PYSPARK:
      case FLINK:
      case BEAM_FLINK:
        //Get all the executions that are in a non-final state, should be only one.
        List<Execution> executions = execFacade.findByJobAndNotFinished(job);
        if (executions != null && !executions.isEmpty()) {
          for (Execution execution : executions) {
            //An execution when it's initializing might not have an appId in hopsworks
            if (execution.getAppId() != null) {
              killExecution(job, execution);
            }
          }
          return execFacade.findById(executions.get(0).getId());
        }
        return null;
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
    }
  }
  
  public void killExecution(Jobs job, Execution execution) throws JobException {
    YarnClientWrapper yarnClientWrapper = null;
    try {
      yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
      yarnClientWrapper.getYarnClient().killApplication(ApplicationId.fromString(execution.getAppId()));
      async.getYarnExecutionFinalizer().removeAllNecessary(execution);
    } catch (IOException | YarnException ex) {
      LOGGER.log(Level.SEVERE,
        "Could not kill job for job:" + job.getName() + "with appId:" + execution.getAppId(), ex);
      throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, Level.WARNING, ex.getMessage(), null, ex);
    } finally {
      ycs.closeYarnClient(yarnClientWrapper);
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
