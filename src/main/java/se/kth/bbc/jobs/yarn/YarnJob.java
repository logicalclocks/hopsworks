package se.kth.bbc.jobs.yarn;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
public abstract class YarnJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(YarnJob.class.getName());

  private static final int DEFAULT_MAX_STATE_POLL_RETRIES = 10;
  private static final int DEFAULT_POLL_TIMEOUT_INTERVAL = 1; //in seconds

  protected YarnRunner runner;
  private YarnMonitor monitor = null;

  private String stdOutFinalDestination, stdErrFinalDestination;
  private boolean started = false;
  private String hdfsUser = null;

  private JobState finalState = null;

  /**
   *
   * @param job
   * @param user
   * @param services
   * @param hadoopDir
   * @throws IllegalArgumentException If the JobDescription does not contain a
   * YarnJobConfiguration object.
   */
  public YarnJob(JobDescription job, AsynchronousJobExecutor services,
          Users user, String hadoopDir) {
    super(job, services, user, hadoopDir);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a YarnJobConfiguration object. Received class: "
              + job.getJobConfig().getClass());
    }
    try {
      hdfsUser = UserGroupInformation.getCurrentUser().getUserName();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new IllegalArgumentException(
              "Exception while trying to retrieve hadoop User Group Information: "
              + ex.getMessage());
    }
    logger.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
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

  protected final boolean appFinishedSuccessfully() {
    return finalState == JobState.FINISHED;
  }

  protected final JobState getFinalState() {
    if (finalState == null) {
      finalState = JobState.FAILED;
    }
    return finalState;
  }

  /**
   * Start the YARN application master.
   * <p/>
   * @return True if the AM was started, false otherwise.
   * @throws IllegalStateException If the YarnRunner has not been set yet.
   */
  protected final boolean startApplicationMaster() throws IllegalStateException {
    if (runner == null) {
      throw new IllegalStateException(
              "The YarnRunner has not been initialized yet.");
    }
    try {
      updateState(JobState.STARTING_APP_MASTER);
      monitor = runner.startAppMaster();
      started = true;
      updateExecution(null, -1, null, null, monitor.getApplicationId().
              toString(), null, null);
      return true;
    } catch (AccessControlException ex) {
      logger.log(Level.SEVERE, "Permission denied:- {0}", ex.getMessage());
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } catch (YarnException | IOException e) {
      logger.log(Level.SEVERE,
              "Failed to start application master for execution "
              + getExecution()
              + ". Aborting execution",
              e);
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    }
  }

  /**
   * Monitor the state of the job.
   * <p/>
   * @return True if monitoring succeeded all the way, false if failed in
   * between.
   */
  protected final boolean monitor() {
    try (YarnMonitor r = monitor.start()) {
      if (!started) {
        throw new IllegalStateException(
                "Trying to monitor a job that has not been started!");
      }
      YarnApplicationState appState;
      int failures;
      try {
        appState = r.getApplicationState();
        updateState(JobState.getJobState(appState));
        //count how many consecutive times the state could not be polled. Cancel if too much.
        failures = 0;
      } catch (YarnException | IOException ex) {
        logger.log(Level.WARNING,
                "Failed to get application state for execution"
                + getExecution(), ex);
        appState = null;
        failures = 1;
      }

      //Loop as long as the application is in a running/runnable state
      while (appState != YarnApplicationState.FAILED && appState
              != YarnApplicationState.FINISHED && appState
              != YarnApplicationState.KILLED && failures
              <= DEFAULT_MAX_STATE_POLL_RETRIES) {
        //wait to poll another time
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime)
                < DEFAULT_POLL_TIMEOUT_INTERVAL * 1000) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            //not much...
          }
        }

        try {
          appState = r.getApplicationState();
          updateState(JobState.getJobState(appState));
          failures = 0;
        } catch (YarnException | IOException ex) {
          failures++;
          logger.log(Level.WARNING,
                  "Failed to get application state for execution "
                  + getExecution() + ". Tried " + failures + " time(s).", ex);
        }
      }

      if (failures > DEFAULT_MAX_STATE_POLL_RETRIES) {
        try {
          logger.log(Level.SEVERE,
                  "Killing application, {0}, because unable to poll for status.",
                  getExecution());
          r.cancelJob();
          updateState(JobState.KILLED);
          finalState = JobState.KILLED;
        } catch (YarnException | IOException ex) {
          logger.log(Level.SEVERE,
                  "Failed to cancel execution, " + getExecution()
                  + " after failing to poll for status.", ex);
          updateState(JobState.FRAMEWORK_FAILURE);
          finalState = JobState.FRAMEWORK_FAILURE;
        }
        return false;
      }
      finalState = JobState.getJobState(appState);
      return true;
    }
  }

  /**
   * Copy the AM logs to their final destination.
   */
  protected void copyLogs() {
    try {
      if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs()) {
          services.getFileOperations(hdfsUser).copyToHDFSFromLocal(true, runner.
                  getStdOutPath(),
                  stdOutFinalDestination);
        } else {
          services.getFileOperations(hdfsUser).renameInHdfs(runner.
                  getStdOutPath(),
                  stdOutFinalDestination);
        }
      }
      if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs()) {
          services.getFileOperations(hdfsUser).copyToHDFSFromLocal(true, runner.
                  getStdErrPath(),
                  stdErrFinalDestination);
        } else {
          services.getFileOperations(hdfsUser).renameInHdfs(runner.
                  getStdErrPath(),
                  stdErrFinalDestination);
        }
      }
      updateExecution(null, -1, stdOutFinalDestination, stdErrFinalDestination,
              null, null, null);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Exception while trying to write logs for execution "
              + getExecution() + " to HDFS.", e);
    }
  }

  @Override
  protected void runJob() {
    // Try to start the AM
    boolean proceed = startApplicationMaster();

    if (!proceed) {
      return;
    }
    proceed = monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    copyLogs();
    updateState(getFinalState());
  }
}
