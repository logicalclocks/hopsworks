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
