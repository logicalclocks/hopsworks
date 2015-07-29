package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.adam.AdamJob;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.user.model.Users;

/**
 * Acts as the interaction point between the Adam frontend and backend.
 * <p>
 * @author stig
 */
@Stateless
public class AdamController {

  private static final Logger logger = Logger.getLogger(AdamController.class.
          getName());

  @EJB
  private FileOperations fops;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;

  /**
   * Start an execution of the given job, ordered by the given User.
   * <p>
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Adam is not set up properly.
   * @throws IllegalArgumentException If the JobDescription is not set up
   * properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   */
  public Execution startJob(JobDescription job, Users user) throws
          IllegalStateException,
          IllegalArgumentException, IOException, NullPointerException {
    //First: do some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.ADAM) {
      throw new IllegalArgumentException(
              "The given job does not represent an Adam job.");
    } else if (!areJarsAvailable()) {
      //Check if all the jars are available
      throw new IllegalStateException(
              "Some ADAM jars are not in HDFS and could not be copied over.");
    }
    //Get to starting the job
    AdamJob adamjob = new AdamJob(job, submitter, user);
    Execution jh = adamjob.requestExecutionId();
    if (jh != null) {
      submitter.startExecution(adamjob);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, job.getProject(),
            user.asUser());
    return jh;
  }

  /**
   * Check if the Spark jar is in HDFS. If it's not, try and copy it there from
   * the local filesystem. If it's still not there, then return false.
   * <p>
   * @return
   */
  private boolean areJarsAvailable() {
    try {
      boolean adamJarMissing = false;
      for (String s : Constants.ADAM_HDFS_JARS) {
        if (!fops.exists(s)) {
          adamJarMissing = true;
          logger.log(Level.WARNING, "Missing Adam jar: {0}", s);
        }
      }
      if (adamJarMissing) {
        return false;
      }
    } catch (IOException e) {
      return false;
    }

    boolean isInHdfs;
    try {
      isInHdfs = fops.exists(Constants.DEFAULT_SPARK_JAR_HDFS_PATH);
    } catch (IOException e) {
      //Can't connect to HDFS: return false
      return false;
    }
    if (isInHdfs) {
      return true;
    }

    File localSparkJar = new File(Constants.DEFAULT_SPARK_JAR_PATH);
    if (localSparkJar.exists()) {
      try {
        fops.copyToHDFSFromLocal(false, Constants.DEFAULT_SPARK_JAR_PATH,
                Constants.DEFAULT_SPARK_JAR_HDFS_PATH);
      } catch (IOException e) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }
}
