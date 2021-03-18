/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.jobs.execution;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.yarn.YarnExecutionFinalizer;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jobs.yarn.YarnMonitor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.host.ServiceStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.jobs.history.YarnApplicationstate;
import io.hops.hopsworks.persistence.entity.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.persistence.entity.project.PaymentType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

public abstract class AbstractExecutionController implements ExecutionController {
  
  private static final Logger LOGGER = Logger.getLogger(AbstractExecutionController.class.getName());
  private static final String REMOTE_PROTOCOL = "hdfs://";
  
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
  private ExecutionFacade executionFacade;
  @EJB
  private YarnClientService ycs;
  @EJB
  private YarnApplicationAttemptStateFacade appAttemptStateFacade;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private YarnExecutionFinalizer yarnExecutionFinalizer;
  @EJB
  private HostServicesFacade hostServicesFacade;
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Execution start(Jobs job, String args, Users user)
    throws JobException, GenericException, ServiceException, ProjectException {
  
    // A user should not be able to start a job if the project is prepaid and it doesn't have quota.
    if(job.getProject().getPaymentType().equals(PaymentType.PREPAID)){
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(job.getProject().getName());
      if(projectQuota == null || projectQuota.getQuotaRemaining() <= 0){
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_ERROR, Level.FINE);
      }
    }

    //Check if checking for nodemanager status is enabled
    //If enabled and nodemanagers are all offline throw an JobException exception
    if(settings.isCheckingForNodemanagerStatusEnabled() && job.getJobType() != JobType.PYTHON) {
      hostServicesFacade.findServices("nodemanager").stream().filter(s -> s.getStatus()
              == ServiceStatus.Started).findFirst().orElseThrow(() ->
              new JobException(RESTCodes.JobErrorCode.NODEMANAGERS_OFFLINE, Level.SEVERE));
    }

    Execution exec;
    switch (job.getJobType()) {
      case FLINK:
        //Materialize certs
        return flinkController.startJob(job, user);
      case SPARK:
        exec = sparkController.startJob(job, args, user);
        if (exec == null) {
          throw new IllegalArgumentException("Problem getting execution object for: " + job.getJobType());
        }
        SparkJobConfiguration config = (SparkJobConfiguration) job.getJobConfig();
        String path = config.getAppPath();
        String pathOfInode;
        try {
          pathOfInode = Utils.prepPath(path);
        } catch (UnsupportedEncodingException ex) {
          throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, Level.FINE,
            "Job name: " + job.getName(), ex.getMessage(), ex);
        }
        Inode inode = inodeController.getInodeAtPath(pathOfInode);
        String inodeName = inode.getInodePK().getName();
      
        activityFacade.persistActivity(ActivityFacade.EXECUTED_JOB + inodeName, job.getProject(), user,
          ActivityFlag.JOB);
        break;
      case PYSPARK:
        if(job.getProject().getPythonEnvironment() == null){
          throw new ProjectException(RESTCodes.ProjectErrorCode.ANACONDA_NOT_ENABLED, Level.FINEST);
        }
        exec = sparkController.startJob(job, args, user);
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
    //Get all the executions that are in a non-final state
    List<Execution> executions = executionFacade.findByJobAndNotFinished(job);
    if (executions != null && !executions.isEmpty()) {
      for (Execution execution : executions) {
        stopExecution(execution);
      }
      return executionFacade.findById(executions.get(0).getId())
        .orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND,
          FINE, "Execution: " + executions.get(0).getId()));
    }
    return null;
  }
  
  
  public Execution stopExecution(Integer id) throws JobException {
    return stopExecution(
      executionFacade.findById(id).orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND,
        FINE, "Execution: " + id)));
  }
  
  public Execution stopExecution(Execution execution) throws JobException {
    //An execution when it's initializing might not have an appId in hopsworks
    if(execution.getAppId() != null && JobState.getRunningStates().contains(execution.getState())) {
      YarnClientWrapper yarnClientWrapper = null;
      try {
        yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
        yarnClientWrapper.getYarnClient().killApplication(ApplicationId.fromString(execution.getAppId()));
        yarnExecutionFinalizer.removeAllNecessary(execution);
        return executionFacade.findById(execution.getId())
          .orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND,
            FINE, "Execution: " + execution.getId()));
      } catch (IOException | YarnException ex) {
        LOGGER.log(Level.SEVERE,
          "Could not kill job for job:" + execution.getJob().getName() + "with appId:" + execution.getAppId(), ex);
        throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, Level.WARNING, ex.getMessage(), null, ex);
      } finally {
        ycs.closeYarnClient(yarnClientWrapper);
      }
    }
    return execution;
  }
  
  public Execution authorize(Jobs job, Integer id) throws JobException {
    Execution execution =
      executionFacade.findById(id).orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND,
      FINE, "Execution: " + id));
    if (execution == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, Level.FINE,
        "execution with id: " + id + " does not belong to job: " + job.getName() + " or does not exist");
    } else {
      if (!job.getExecutions().contains(execution)) {
        throw new JobException(RESTCodes.JobErrorCode.UNAUTHORIZED_EXECUTION_ACCESS, Level.FINE);
      }
    }
    return execution;
  }
  
  @Override
  public void delete(Execution execution) throws JobException {
    executionFacade.remove(execution);
  }
  
  //====================================================================================================================
  // Execution logs
  //====================================================================================================================
  @Override
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
        Project project = execution.getJob().getProject();
        stdPath = path.split(project.getName())[1];
        int fileIndex = stdPath.lastIndexOf('/');
        String stdDirPath = stdPath.substring(0, fileIndex);
        dto.setPath(Settings.DIR_ROOT + File.separator + project.getName() + stdDirPath + File.separator +  "std" +
          dto.getType().getName().toLowerCase() + ".log");
        if (dfso.listStatus(new org.apache.hadoop.fs.Path(hdfsPath))[0].getLen() > settings.getJobLogsDisplaySize()) {
          dto.setLog("Log is too big to display in browser. Click on the download button to get the log file.");
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
        String logMsg = "No log available.";
        if ( execution.getJob().getJobType() == JobType.PYTHON){
          logMsg+= " If job failed instantaneously, please check again later or try running the job again. Log " +
            "aggregation can take a few minutes to complete.";
          dto.setLog(logMsg);
        }
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
  
  @Override
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

  @Override
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
  @Override
  public List<YarnAppUrlsDTO> getTensorBoardUrls(Users user, String appId, Project project)
    throws JobException {
    List<YarnAppUrlsDTO> urls = new ArrayList<>();
    DistributedFileSystemOps udfso = null;
    
    try {
      String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
      
      udfso = dfs.getDfsOps(hdfsUser);
      FileStatus[] statuses = udfso.getFilesystem().globStatus(
        new org.apache.hadoop.fs.Path(
          "/Projects/" + project.getName() + "/Experiments/" + appId + "*/TensorBoard.*"));
      
      for (FileStatus status : statuses) {
        LOGGER.log(Level.FINE, "Reading TensorBoard for: {0}", status.getPath());
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
          LOGGER.log(Level.WARNING, "Problem reading file with TensorBoard address from HDFS: " + e.getMessage());
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
