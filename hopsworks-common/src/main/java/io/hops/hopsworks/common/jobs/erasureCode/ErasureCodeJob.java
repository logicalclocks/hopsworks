package io.hops.hopsworks.common.jobs.erasureCode;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.dao.user.Users;

public class ErasureCodeJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(ErasureCodeJob.class.
          getName());
  private ErasureCodeJobConfiguration jobConfig;

  public ErasureCodeJob(JobDescription job, AsynchronousJobExecutor services,
          Users user,
          String hadoopDir, String nameNodeIpPort) {

    super(job, services, user, hadoopDir, nameNodeIpPort);

    if (!(job.getJobConfig() instanceof ErasureCodeJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain an ErasureCodeJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }

    this.jobConfig = (ErasureCodeJobConfiguration) job.getJobConfig();
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
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
