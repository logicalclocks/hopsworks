package se.kth.bbc.fileoperations;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author vangelis
 */
public class ErasureCodeJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(ErasureCodeJob.class.
          getName());
  
  private ErasureCodeJobConfiguration jobConfig;

  public ErasureCodeJob(JobDescription job,
          Users user, AsynchronousJobExecutor services) {

    super(job, services, user);

    if (!(job.getJobConfig() instanceof ErasureCodeJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain an ErasureCodeJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }

    this.jobConfig = (ErasureCodeJobConfiguration) job.getJobConfig();
  }

  @Override
  protected boolean setupJob() {
    if (jobConfig.getAppName() == null || jobConfig.getAppName().isEmpty()) {
      jobConfig.setAppName("Untitled Erasure coding Job");
    }

    return true;
  }

  @Override
  protected void runJob() {

    boolean jobSucceeded = false;

    try {
      //do compress the file
      jobSucceeded = this.services.getFileOperations().compress(this.jobConfig.
              getFilePath());
    } catch (IOException | NotFoundException e) {
      jobSucceeded = false;
    }
    
    if(jobSucceeded){
      //TODO: push a message to the messaging service
      logger.log(Level.INFO, "File compression was successful");
      return;
    }
    
    //push message to the messaging service
    logger.log(Level.INFO, "File compression was not successful");
  }

  @Override
  protected void cleanup() {
  }

}
