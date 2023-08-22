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

package io.hops.hopsworks.common.jobs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SparkConfigurationUtil;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.persistence.entity.jobs.configuration.flink.FlinkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.eclipse.persistence.exceptions.DatabaseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobController {
  
  @EJB
  private JobFacade jobFacade;
  @EJB
  private JobScheduler scheduler;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private SparkController sparkController;
  @Inject
  private ExecutionController executionController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private ProjectUtils projectUtils;

  private static final Logger LOGGER = Logger.getLogger(JobController.class.getName());

  private String getJobRootFolder(String projectName) {
    return "hdfs://" + Utils.getProjectPath(projectName) + Settings.PROJECT_STAGING_DIR + "/jobs";
  }

  public String getJobFolder(String projectName, String jobName) {
    return getJobRootFolder(projectName)  + "/" + jobName;
  }

  public String createJobFolder(Project project, Users user, String jobName) throws JobException {
    String jobUri = getJobFolder(project.getName(), jobName);
    return HopsUtils.createDirectory(dfs, hdfsUsersController.getHdfsUserName(project, user), jobUri);
  }
  
  public Jobs putJob(Users user, Project project, Jobs job, JobConfiguration config) throws JobException {
    try {
      if(config.getJobType() == JobType.SPARK || config.getJobType() == JobType.PYSPARK) {
        SparkConfigurationUtil sparkConfigurationUtil = new SparkConfigurationUtil();
        SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration)config;
        sparkConfigurationUtil.validateExecutorMemory(sparkJobConfiguration.getExecutorMemory(), settings);
      }
      job = jobFacade.put(user, project, config, job);
    } catch (IllegalStateException ise) {
      if (ise.getCause() instanceof JAXBException) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_CONFIGURATION_CONVERT_TO_JSON_ERROR, Level.FINE,
          "Unable to create json from JobConfiguration", ise.getMessage(), ise);
      } else {
        throw ise;
      }
    }
    
    if (config.getSchedule() != null) {
      scheduler.scheduleJobPeriodic(job);
    }
  
    activityFacade.persistActivity(ActivityFacade.CREATED_JOB + getJobNameForActivity(job.getName()), project, user,
      ActivityFlag.JOB);
    return job;
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateSchedule(Project project, Jobs job, ScheduleDTO schedule, Users user) throws JobException {
    boolean isScheduleUpdated = jobFacade.updateJobSchedule(job.getId(), schedule);
    if (isScheduleUpdated) {
      job.getJobConfig().setSchedule(schedule);
      scheduler.scheduleJobPeriodic(job);
      activityFacade.persistActivity(ActivityFacade.SCHEDULED_JOB + getJobNameForActivity(job.getName()), project, user,
        ActivityFlag.JOB);
    } else {
      throw new JobException(RESTCodes.JobErrorCode.JOB_SCHEDULE_UPDATE, Level.WARNING,
        "Schedule is not updated in the database for jobid: " + job.getId());
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public boolean unscheduleJob(Jobs job) {
    if (job.getJobConfig().getSchedule() != null) {
      boolean status = scheduler.unscheduleJob(job);
      job.getJobConfig().setSchedule(null);
      jobFacade.updateJobSchedule(job.getId(), null);
      if (!status) {
        LOGGER.log(Level.WARNING, "Schedule does not exist in the scheduler for jobid {0}", job.getId());
      }
    }
    return scheduler.unscheduleJob(job);
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void deleteJob(Jobs job, Users user) throws JobException {
    // Delete schedule V1
    if (job.getJobConfig().getSchedule() != null) {
      unscheduleJob(job);
    }
    
    //Kill running execution of this job (if any)
    executionController.stop(job);
    // Wait till execution is in a final state
    List<Execution> nonFinishedExecutions = executionFacade.findByJobAndNotFinished(job);
    LOGGER.log(Level.FINE, "nonFinishedExecutions:" + nonFinishedExecutions);
    int sleep = 2000;
    int timeout = 60;
    int retries = timeout * 1000 / sleep;
    int i = 0;
    while (!nonFinishedExecutions.isEmpty() && i < retries) {
      LOGGER.log(Level.INFO, "waiting for executions:" + nonFinishedExecutions);
      // Wait a few seconds till execution in final state
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException ex) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_DELETION_ERROR,
                               Level.WARNING,
                               "Interrupted while waiting to stop the job's executions. Job: " + job.getName(),
                               ex.getMessage(),
                               ex);
      }
      nonFinishedExecutions = executionFacade.findByJobAndNotFinished(job);
      i++;
    }

    try {
      LOGGER.log(Level.FINE, "Request to delete job name ={0} job id ={1}",
        new Object[]{job.getName(), job.getId()});
      jobFacade.removeJob(job);
      String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
      cleanupJobDatasetResources(job, username, dfs);
      LOGGER.log(Level.FINE, "Deleted job name ={0} job id ={1}", new Object[]{job.getName(), job.getId()});
      String activityMessage = ActivityFacade.DELETED_JOB + job.getName();
      if (!nonFinishedExecutions.isEmpty()) {
        activityMessage += " with pending executions: " + nonFinishedExecutions;
      }
      activityFacade.persistActivity(activityMessage, job.getProject(), user.getEmail(), ActivityFlag.JOB);
    } catch (DatabaseException ex) {
      LOGGER.log(Level.SEVERE, "Job cannot be deleted job name ={0} job id ={1}",
        new Object[]{job.getName(), job.getId()});
      throw new JobException(RESTCodes.JobErrorCode.JOB_DELETION_ERROR, Level.SEVERE, ex.getMessage(), null, ex);
    }
  }
  
  public Jobs getJob(Project project, String name) throws JobException {
    Jobs job = findJob(project, name);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "jobId:" + name);
    }
    return job;
  }
  
  private Jobs findJob(Project project, String name) throws JobException {
    if(Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("job name was not provided or it was not set.");
    }
    return jobFacade.findByProjectAndName(project, name);
  }

  public List<Jobs> getJobsWithJobNameRegex(Project project, String jobNameRegex) {
    if(Strings.isNullOrEmpty(jobNameRegex)) {
      throw new IllegalArgumentException("job name regex was not provided or it was not set.");
    }
    return jobFacade.findByProjectAndJobNameRegex(project, jobNameRegex);
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public JobConfiguration inspectProgram(String path, Project project, Users user, JobType jobType)
          throws JobException {
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      LOGGER.log(Level.FINE, "Inspecting executable job program by {0} at path: {1}", new Object[]{username, path});

      JobConfiguration jobConf = getConfiguration(project, jobType, true);

      switch (jobType) {
        case SPARK:
        case PYSPARK:
          if (Strings.isNullOrEmpty(path) || !(path.endsWith(".jar") || path.endsWith(".py")
              || path.endsWith(".ipynb"))) {
            throw new IllegalArgumentException("Path does not point to a .jar, .py or .ipynb file.");
          }
          return sparkController.inspectProgram((SparkJobConfiguration)jobConf, path, udfso);
        case PYTHON:
          if (Strings.isNullOrEmpty(path) || !(path.endsWith(".py") || path.endsWith(".ipynb") || path.endsWith(
              ".egg") || path.endsWith(".zip"))) {
            throw new IllegalArgumentException("Path does not point to a .py or .ipynb or .egg file.");
          }
          return jobConf;
        case DOCKER:
        case FLINK:
          return jobConf;
        default:
          throw new IllegalArgumentException("Job type not supported: " + jobType);
      }
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public JobConfiguration getConfiguration(Project project, JobType jobType, boolean useDefaultConfig) {

    Optional<DefaultJobConfiguration> defaultConfig;
    /**
     * The Spark and PySpark configuration is stored in the same configuration entry in the
     * database for DefaultJobConfiguration. Namely in a PySpark configuration. We infer the JobType based on if
     * you set a .jar or .py. However when creating the DefaultJobConfiguration, as part of the PK the JobType
     * needs to be set. So for now PySpark/Spark shares the same configuration.
     */
    if(jobType.equals(JobType.SPARK) || jobType.equals(JobType.PYSPARK)) {
      defaultConfig = project.getDefaultJobConfigurationCollection().stream()
        .filter(conf -> conf.getDefaultJobConfigurationPK().getType().equals(JobType.PYSPARK))
        .findFirst();
      defaultConfig.ifPresent(defaultJobConfiguration ->
        ((SparkJobConfiguration) defaultJobConfiguration.getJobConfig()).setMainClass(null));
    } else {
      defaultConfig = project.getDefaultJobConfigurationCollection().stream()
        .filter(conf -> conf.getDefaultJobConfigurationPK().getType().equals(jobType))
        .findFirst();
    }
    if(defaultConfig.isPresent()) {
      return defaultConfig.get().getJobConfig();
    } else if(useDefaultConfig) {
      switch (jobType) {
        case SPARK:
        case PYSPARK:
          return new SparkJobConfiguration();
        case PYTHON:
          PythonJobConfiguration pyConfig = new PythonJobConfiguration();
          pyConfig.setResourceConfig(projectUtils.buildDockerResourceConfig());// HOPSWORKS-2660
          return pyConfig;
        case FLINK:
          return new FlinkJobConfiguration();
        case DOCKER:
          return projectUtils.buildDockerJobConfiguration(); // HOPSWORKS-2660
        default:
          throw new IllegalArgumentException("Job type not supported: " + jobType);
      }
    } else {
      return null;
    }
  }

  public void versionProgram(SparkJobConfiguration job, Project project, Users user, Path path)
      throws JobException {
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersController.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      versionProgram(job.getAppPath(), udfso, path);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void versionProgram(String appPath, DistributedFileSystemOps udfso, Path path) throws JobException {
    try {
      udfso.copyInHdfs(new Path(appPath), path);
    } catch (IOException ioe) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_PROGRAM_VERSIONING_FAILED, Level.FINEST,
        "path: " + appPath, "versioning failed", ioe);
    }
  }
  
  private String getJobNameForActivity(String jobName) {
    String activityJobMsg = jobName;
    if (jobName.length() > 60) {
      activityJobMsg = jobName.substring(0, 60) + "...";
    }
    return activityJobMsg;
  }

  private void cleanupJobDatasetResources(Jobs job, String hdfsUsername, DistributedFsService dfs)
      throws JobException {
    String jobPath = getJobFolder(job.getProject().getName(), job.getName());
    try {
      HopsUtils.removeFiles(jobPath, hdfsUsername, dfs);
    } catch (DatasetException e) {
      String msg = "failed to cleanup job dataset resources";
      throw new JobException(RESTCodes.JobErrorCode.JOB_DELETION_ERROR, Level.INFO, msg, msg, e);
    }
  }
}
