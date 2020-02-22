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
package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.JobException;

import org.apache.hadoop.yarn.client.api.YarnClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the execution of a Spark job: run job, update history object.
 * <p>
 */
public class SparkJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(SparkJob.class.getName());
  private SparkYarnRunnerBuilder runnerbuilder;

  SparkJob(Jobs job, AsynchronousJobExecutor services,
    Users user, final String hadoopDir,
    String jobUser, YarnJobsMonitor jobsMonitor, Settings settings, String kafkaBrokersString) {
    super(job, services, user, jobUser, hadoopDir, jobsMonitor, settings, kafkaBrokersString);
    if (!(job.getJobConfig() instanceof SparkJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a SparkJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) throws JobException {
    super.setupJob(dfso, yarnClient);
    SparkJobConfiguration jobconfig = (SparkJobConfiguration) jobs.getJobConfig();
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    //If runnerbuilder is not null, it has been instantiated by child class,
    if (runnerbuilder == null) {
      runnerbuilder = new SparkYarnRunnerBuilder(jobs);
      runnerbuilder.setJobName(jobconfig.getAppName());
    }
    //Set Kafka params
    runnerbuilder.setServiceProps(serviceProps);
    if(jobconfig.getLocalResources() != null) {
      runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    }
    //Set project specific resources, i.e. Kafka certificates
    runnerbuilder.addExtraFiles(projectLocalResources);
    if (jobSystemProperties != null && !jobSystemProperties.isEmpty()) {
      for (Entry<String, String> jobSystemProperty : jobSystemProperties.
          entrySet()) {
        runnerbuilder.addSystemProperty(jobSystemProperty.getKey(),
            jobSystemProperty.getValue());
      }
    }

    String stdOutFinalDestination = Utils.getProjectPath(jobs.getProject().getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getProjectPath(jobs.getProject().getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);

    try {
      runner = runnerbuilder.
          getYarnRunner(jobs.getProject(),
              jobUser,
              services, services.getFileOperations(hdfsUser.getUserName()), yarnClient, settings, kafkaBrokersString);
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

  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
  }

}
