package se.kth.bbc.jobs.spark;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.exceptions.YarnException;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;

/**
 * Orchestrates the execution of a Spark job: run job, update history
 * object.
 * @author stig
 */
public final class SparkJob extends YarnJob {
  private static final Logger logger = Logger.getLogger(SparkJob.class.getName());

  public SparkJob(JobHistoryFacade facade, YarnRunner runner,
          FileOperations fops) {
    super(facade, runner, fops);
  }

  @Override
  public HopsJob getInstance(JobHistory jh) throws IllegalArgumentException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void runJobInternal() {
    //Update job history object
    super.updateArgs();

    //Keep track of time and start job
    long startTime = System.currentTimeMillis();
    //Try to start the AM
    boolean proceed = super.startJob();
    //If success: monitor running job
    if (!proceed) {
      return;
    }
    proceed = super.monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    super.copyLogs();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    try {
      getJobHistoryFacade().update(getJobId(), JobState.getJobState(
              getRunner().getApplicationState()), duration);
    } catch (YarnException | IOException ex) {
      logger.log(Level.SEVERE, "Error while getting final state for job "
              + getJobId() + ". Assuming failed", ex);
      getJobHistoryFacade().update(getJobId(),
              JobState.FRAMEWORK_FAILURE);
    }
  }

}
