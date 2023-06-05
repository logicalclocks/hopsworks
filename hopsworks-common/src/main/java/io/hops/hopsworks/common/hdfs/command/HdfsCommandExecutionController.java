/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.hdfs.command;

import io.hops.hopsworks.common.dao.hdfs.command.HdfsCommandExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.hdfs.command.Command;
import io.hops.hopsworks.persistence.entity.hdfs.command.HdfsCommandExecution;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HdfsCommandExecutionController {

  @EJB
  private HdfsCommandExecutionFacade hdfsCommandExecutionFacade;
  @EJB
  private JobController jobController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private Settings settings;
  @Inject
  private ExecutionController executionController;

  private final static String JOB_NAME = "hdfs_file_operations";

  
  public HdfsCommandExecution setupAndStartJob(Users user, Project project, Command command, Path src, Path dest,
                                               ArchiveFormat format, boolean overwrite)
         throws JobException, ProjectException, ServiceException, GenericException {
    Optional<HdfsCommandExecution> hdfsCommandExecutionOptional =
      hdfsCommandExecutionFacade.findBySrcPath(src.toString());
    if (hdfsCommandExecutionOptional.isPresent() &&
      JobFinalStatus.UNDEFINED.equals(hdfsCommandExecutionOptional.get().getExecution().getFinalStatus())) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, Level.FINE,
        "There is a running " + command.getJobCmd() + " job on this file.");
    }
    String jobArgs = getJobArgs(command.getJobCmd(), src, dest, format, overwrite);
    Jobs job = configureJob(user, project);
    Execution execution = executionController.start(job, jobArgs, user);
    if (hdfsCommandExecutionOptional.isPresent()) {
      HdfsCommandExecution hdfsCommandExecution = hdfsCommandExecutionOptional.get();
      hdfsCommandExecution.setCommand(command);
      hdfsCommandExecution.setExecution(execution);
      hdfsCommandExecution.setSubmitted(new Date());
      hdfsCommandExecution.setSrcPath(src.toString());
      hdfsCommandExecutionFacade.update(hdfsCommandExecution);
    } else {
      HdfsCommandExecution hdfsCommandExecution = new HdfsCommandExecution(execution, command, src.toString());
      hdfsCommandExecutionFacade.save(hdfsCommandExecution);
    }
    return hdfsCommandExecutionFacade.findByExecution(execution)
      .orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, Level.FINE, "Failed to start " +
        "execution"));
  }

  private Jobs configureJob(Users user, Project project) throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, JOB_NAME);
    if (job != null) {
      if (!isJobValid(job)) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, Level.FINE, "Another job with the same name " +
          "already exist.");
      }
      return job;
    }
    SparkJobConfiguration sparkJobConfiguration = new SparkJobConfiguration();

    sparkJobConfiguration.setAppName(JOB_NAME);
    sparkJobConfiguration.setMainClass(Settings.SPARK_PY_MAINCLASS);
    sparkJobConfiguration.setAppPath(settings.getHdfsFileOpJobUtil());
    sparkJobConfiguration.setAmMemory(settings.getHdfsFileOpJobDriverMemory());
    sparkJobConfiguration.setExecutorInstances(0);
    sparkJobConfiguration.setDynamicAllocationEnabled(false);

    return jobController.putJob(user, project, null, sparkJobConfiguration);
  }

  private String getJobArgs(String op, Path src, Path dest, ArchiveFormat format, boolean overwrite) {
    String destArg = dest == null ? "" : " -dest " + dest;
    String formatArg = format == null ? "" : " -format " + format.getJobFormat();
    String overwriteArg = overwrite ? " -overwrite True" : "";
    return "-op " + op + " -src " + src + destArg + formatArg + overwriteArg;
  }

  /**
   * Check if job is using the correct py file. If user creates a job with the same name.
   */
  private boolean isJobValid(Jobs job) {
    // JobConfig can not be null
    SparkJobConfiguration sparkJobConfiguration = (SparkJobConfiguration) job.getJobConfig();
    if (JobType.PYSPARK.equals(job.getJobType()) && sparkJobConfiguration.getAppPath() != null) {
      int index = sparkJobConfiguration.getAppPath().indexOf('-');
      String appPath =
        index > -1 ? sparkJobConfiguration.getAppPath().substring(0, index) : sparkJobConfiguration.getAppPath();
      return settings.getHdfsFileOpJobUtil().startsWith(appPath);
    }
    return false;
  }
}
