/*
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
 *
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
