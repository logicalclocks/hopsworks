package se.kth.bbc.jobs.yarn;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobState;

/**
 *
 * @author stig
 */
public class YarnJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(YarnJob.class.getName());

  private static final int DEFAULT_MAX_STATE_POLL_RETRIES = 10;
  private static final int DEFAULT_POLL_TIMEOUT_INTERVAL = 1; //in seconds

  private final YarnRunner runner;
  private YarnMonitor monitor = null;
  private final FileOperations fops;

  private String stdOutFinalDestination, stdErrFinalDestination;
  private boolean started = false;

  private JobState finalState = null;

  public YarnJob(JobHistoryFacade facade, YarnRunner runner, FileOperations fops) {
    super(facade);
    this.runner = runner;
    this.fops = fops;
  }

  public final void setStdOutFinalDestination(String stdOutFinalDestination) {
    this.stdOutFinalDestination = stdOutFinalDestination;
  }

  public final void setStdErrFinalDestination(String stdErrFinalDestination) {
    this.stdErrFinalDestination = stdErrFinalDestination;
  }

  protected final void updateArgs() {
    super.updateArgs(runner.getAmArgs());
  }

  protected final boolean appFinishedSuccessfully() {
    return finalState == JobState.FINISHED;
  }

  protected final JobState getFinalState() {
    return finalState;
  }

  protected final YarnRunner getRunner() {
    return runner;
  }

  protected final FileOperations getFileOperations() {
    return fops;
  }

  protected final boolean startJob() {
    try {
      updateState(JobState.STARTING_APP_MASTER);
      monitor = runner.startAppMaster();
      started = true;
      return true;
    } catch (YarnException | IOException e) {
      logger.log(Level.SEVERE,
              "Failed to start application master for job " + getJobId()
              + ". Aborting execution",
              e);
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    }
  }

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
                "Failed to get application state for job id "
                + getJobId(), ex);
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
                  "Failed to get application state for job id "
                  + getJobId() + ". Tried " + failures + " time(s).", ex);
        }
      }

      if (failures > DEFAULT_MAX_STATE_POLL_RETRIES) {
        try {
          logger.log(Level.SEVERE,
                  "Killing application, jobId {0}, because unable to poll for status.",
                  getJobId());
          r.cancelJob();
          updateState(JobState.KILLED);
          finalState = JobState.KILLED;
        } catch (YarnException | IOException ex) {
          logger.log(Level.SEVERE,
                  "Failed to cancel job, jobId " + getJobId()
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

  protected final void copyLogs() {
    try {
      if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs()) {
          fops.copyToHDFSFromPath(runner.getStdOutPath(),
                  stdOutFinalDestination,
                  null);
          getJobHistoryFacade().updateStdOutPath(getJobId(),
                  stdOutFinalDestination);
        } else {
          //TODO: move in HDFS
          getJobHistoryFacade().updateStdOutPath(getJobId(),
                  runner.getStdOutPath());
        }

      }
      if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs()) {
          fops.copyToHDFSFromPath(runner.getStdErrPath(),
                  stdErrFinalDestination,
                  null);
          getJobHistoryFacade().updateStdErrPath(getJobId(),
                  stdErrFinalDestination);
        } else {
          //TODO: move in HDFS
          getJobHistoryFacade().updateStdErrPath(getJobId(),
                  runner.getStdErrPath());
        }
      }
    } catch (IOException e) {
      //TODO: figure out how to handle this
      logger.log(Level.SEVERE, "Exception while trying to write logs for job "
              + getJobId() + " to HDFS.", e);
    }
  }

  @Override
  public HopsJob getInstance(JobHistory jh) throws IllegalArgumentException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void runJobInternal() {
    //Update job history object
    updateArgs();

    //Keep track of time and start job
    long startTime = System.currentTimeMillis();
    // Try to start the AM
    boolean proceed = startJob();

    if (!proceed) {
      return;
    }
    copyLogs();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    getJobHistoryFacade().update(getJobId(), getFinalState(), duration);

  }
}
