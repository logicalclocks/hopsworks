/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.tensorflow;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class TensorFlowJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(TensorFlowJob.class.getName());
  private final String tfUser;
  protected TensorFlowYarnRunnerBuilder runnerbuilder;

  public TensorFlowJob(Jobs job, AsynchronousJobExecutor services, Users user, final String hadoopDir,
      String tfUser, String jobUser, YarnJobsMonitor jobsMonitor, Settings settings) {
    super(job, services, user, jobUser, hadoopDir, jobsMonitor, settings);
    if (!(job.getJobConfig() instanceof TensorFlowJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a TensorFlowJobConfiguration object. Received: " + 
              job.getJobConfig().getClass());
    }
    this.tfUser = tfUser;
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) {
    super.setupJob(dfso, yarnClient);
    TensorFlowJobConfiguration jobconfig = (TensorFlowJobConfiguration) jobs.getJobConfig();

    runnerbuilder = new TensorFlowYarnRunnerBuilder(jobs);
    runnerbuilder.setJobName(jobconfig.getAppName());
    //Check if the user provided application arguments
    if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
      String[] jobArgs = jobconfig.getArgs().trim().split(" ");
      runnerbuilder.addAllJobArgs(jobArgs);
    }
    runnerbuilder.setJobName(jobconfig.getAppName());
    //Set spark runner options
    runnerbuilder.setNumOfPs(jobconfig.getNumOfPs());
    runnerbuilder.setNumOfWorkers(jobconfig.getNumOfWorkers());
    runnerbuilder.setWorkerMemory(jobconfig.getWorkerMemory());
    runnerbuilder.setWorkerVCores(jobconfig.getWorkerVCores());
    runnerbuilder.setWorkerGPUs(jobconfig.getNumOfGPUs());

    //Set Yarn running options
    runnerbuilder.setAmMemory(jobconfig.getAmMemory());
    runnerbuilder.setAmVCores(jobconfig.getAmVCores());
    runnerbuilder.setQueue(jobconfig.getAmQueue());

    runnerbuilder.setServiceProps(serviceProps);
    runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    runnerbuilder.addExtraFiles(projectLocalResources);

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobs.getProject().getName())
        + Settings.TENSORFLOW_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobs.getProject().getName())
        + Settings.TENSORFLOW_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);

    try {
      runner = runnerbuilder.getYarnRunner(jobs.getProject().getName(), tfUser, jobUser, hadoopDir,
          services.getFileOperations(hdfsUser.getUserName()), yarnClient,
          services, settings);

    } catch (Exception e) {
      LOG.log(Level.WARNING,
          "Failed to create YarnRunner.", e);
      try {
        writeToLogs(e.getLocalizedMessage());
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", ex);
      }
      return false;
    }

    return true;
  }

  @Override
  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
  }

  @Override
  protected void stopJob(String appid) {
    //Stop flink cluster first
    try {
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(this.hadoopDir + "/bin/yarn application -kill "
          + appid);
    } catch (IOException ex1) {
      LOG.log(Level.SEVERE, "Unable to stop flink cluster with appID:"
          + appid, ex1);
    }
  }

}
