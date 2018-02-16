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

package io.hops.hopsworks.common.jobs.erasureCode;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.dao.user.Users;
import org.apache.hadoop.yarn.client.api.YarnClient;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;

public class ErasureCodeJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(ErasureCodeJob.class.
          getName());
  private ErasureCodeJobConfiguration jobConfig;

  public ErasureCodeJob(Jobs job, AsynchronousJobExecutor services,
          Users user, String hadoopDir, YarnJobsMonitor jobsMonitor) {

    super(job, services, user, hadoopDir, jobsMonitor);

    if (!(job.getJobConfig() instanceof ErasureCodeJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain an ErasureCodeJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }

    this.jobConfig = (ErasureCodeJobConfiguration) job.getJobConfig();
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) {
    if (jobConfig.getAppName() == null || jobConfig.getAppName().isEmpty()) {
      jobConfig.setAppName("Untitled Erasure coding Job");
    }

    return true;
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso) {
    boolean jobSucceeded = false;
    try {
      //do compress the file
      jobSucceeded = dfso.compress(this.jobConfig.
              getFilePath());
    } catch (IOException | NotFoundException e) {
      jobSucceeded = false;
    }
    if (jobSucceeded) {
      //TODO(Theofilos): push a message to the messaging service
      logger.log(Level.INFO, "File compression was successful");
      return;
    }
    //push message to the messaging service
    logger.log(Level.INFO, "File compression was not successful");
  }

  @Override
  protected void stopJob(String appid) {

  }

  @Override
  protected void cleanup() {
  }

  @Override
  protected void writeToLogs(String message, Exception e) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  protected void writeToLogs(String message) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

}
