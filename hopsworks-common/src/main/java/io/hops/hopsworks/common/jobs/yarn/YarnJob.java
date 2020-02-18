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

package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.JobException;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class YarnJob extends HopsJob {

  private static final Logger LOG = Logger.getLogger(YarnJob.class.getName());

  protected YarnRunner runner;

  private String stdOutFinalDestination, stdErrFinalDestination;
  protected List<LocalResourceDTO> projectLocalResources;
  protected Map<String, String> jobSystemProperties;

  protected final String jobUser;
  protected Settings settings;
  

  /**
   * Constructor for job interacting with the Kafka service.
   *
   * @param job
   * @param user
   * @param services
   * @param jobUser
   * @param hadoopDir
   * @param jobsMonitor
   * @param settings
   * @throws IllegalArgumentException If the Jobs does not contain a
 YarnJobConfiguration object.
   */
  public YarnJob(Jobs job, AsynchronousJobExecutor services,
      Users user, String jobUser, String hadoopDir, YarnJobsMonitor jobsMonitor,
      Settings settings) {
    super(job, services, user, hadoopDir, jobsMonitor);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
          "Job must be a YarnJobConfiguration object. Received class: "
          + job.getJobConfig().getClass());
    }
    LOG.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
    this.jobSystemProperties = new HashMap<>();
    this.projectLocalResources = new ArrayList<>();
    this.jobUser = jobUser;
    this.settings = settings;
  }

  public final void setStdOutFinalDestination(String stdOutFinalDestination) {
    this.stdOutFinalDestination = stdOutFinalDestination;
  }

  public final void setStdErrFinalDestination(String stdErrFinalDestination) {
    this.stdErrFinalDestination = stdErrFinalDestination;
  }

  protected final String getStdOutFinalDestination() {
    return this.stdOutFinalDestination;
  }

  protected final String getStdErrFinalDestination() {
    return this.stdErrFinalDestination;
  }

  /**
   * Start the YARN application master.
   *
   * @param udfso
   * @param dfso
   * @return True if the AM was started, false otherwise.
   * @throws IllegalStateException If the YarnRunner has not been set yet.
   */
  private boolean startApplicationMaster(DistributedFileSystemOps udfso, DistributedFileSystemOps dfso, String args) {
    if (runner == null) {
      throw new IllegalArgumentException(
          "The YarnRunner has not been initialized yet.");
    }
    try {
      ApplicationId appId = runner.startAppMaster(jobs.getProject(), dfso,user.getUsername(), args);
      execution = services.getExecutionFacade().updateFilesToRemove(execution, runner.getFilesToRemove());
      execution = services.getExecutionFacade().updateAppId(execution, appId.toString());
      return true;
    } catch (AccessControlException ex) {
      LOG.log(Level.SEVERE, "Permission denied:- {0}", ex.getMessage());
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to start application master for execution " + execution
          + ". Aborting execution", e);
      writeLog("Failed to start application master for execution " + execution + ". Aborting execution", e, udfso);
      try {
        services.getYarnExecutionFinalizer().removeAllNecessary(execution);
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Failed to remove files for failed execution {0}", execution);
        writeLog("Failed to remove files for failed execution " + execution, ex, udfso);
      }
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } finally {
      if (runner != null) {
        runner.stop(services.getFsService());
      }
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) throws JobException {
    //Check if this job is using Kakfa, and include certificate
    //in local resources
    serviceProps = new ServiceProperties(jobs.getProject().getId(), jobs.getProject().getName(),
        services.getSettings().getRestEndpoint(), jobs.getName(), new ElasticProperties(
        services.getSettings().getElasticRESTEndpoint()));
    return true;
  }

  final EnumSet<YarnApplicationState> finalAppState = EnumSet.of(
      YarnApplicationState.FINISHED, YarnApplicationState.FAILED,
      YarnApplicationState.KILLED);

  private void writeLog(String message, Exception exception, DistributedFileSystemOps udfso) {

    Date date = new Date();
    String dateString = date.toString();
    dateString = dateString.replace(" ", "_").replace(":", "-");
    stdErrFinalDestination = stdErrFinalDestination + jobs.getName() + dateString + "/stderr.log";
    YarnLogUtil.writeLog(udfso, stdErrFinalDestination, message, exception);
    services.getExecutionFacade().updateStdErrPath(execution, stdErrFinalDestination);
  }

  @Override
  protected void writeToLogs(String message, Exception e) throws IOException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = services.getFileOperations(jobUser);
      writeLog(message, e, udfso);
    } finally {
      if (null != udfso) {
        services.getFsService().closeDfsClient(udfso);
      }
    }
  }

  @Override
  protected void writeToLogs(String message) throws IOException {
    writeToLogs(message, null);
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso, String args) {
    // Try to start the AM
    boolean proceed = startApplicationMaster(udfso, dfso, args);

    if (!proceed) {
      return;
    }

  }

}
