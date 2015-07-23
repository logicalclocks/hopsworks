package se.kth.bbc.jobs.jobhistory;

import org.apache.hadoop.yarn.api.records.YarnApplicationState;

/**
 *
 * @author stig
 */
public enum JobState {

  INITIALIZING,
  INITIALIZATION_FAILED,
  FINISHED,
  RUNNING,
  ACCEPTED,
  FAILED,
  KILLED,
  NEW,
  NEW_SAVING,
  SUBMITTED,
  FRAMEWORK_FAILURE,
  STARTING_APP_MASTER,
  APP_MASTER_START_FAILED;

  @Override
  public String toString() {
    switch (this) {
      case INITIALIZING:
        return "Initializing";
      case INITIALIZATION_FAILED:
        return "Initialization failed";
      case FINISHED:
        return "Finished";
      case RUNNING:
        return "Running";
      case ACCEPTED:
        return "Accepted";
      case FAILED:
        return "Failed";
      case KILLED:
        return "Killed";
      case NEW:
        return "New";
      case NEW_SAVING:
        return "New saving";
      case SUBMITTED:
        return "Submitted";
      case FRAMEWORK_FAILURE:
        return "Framework failure";
      case STARTING_APP_MASTER:
        return "Starting Application Master";
      case APP_MASTER_START_FAILED:
        return "AM start failed";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }

  public static JobState getJobState(YarnApplicationState yarnstate) {
    switch (yarnstate) {
      case RUNNING:
        return JobState.RUNNING;
      case ACCEPTED:
        return JobState.ACCEPTED;
      case FAILED:
        return JobState.FAILED;
      case FINISHED:
        return JobState.FINISHED;
      case KILLED:
        return JobState.KILLED;
      case NEW:
        return JobState.NEW;
      case NEW_SAVING:
        return JobState.NEW_SAVING;
      case SUBMITTED:
        return JobState.SUBMITTED;
      default:
        throw new IllegalArgumentException("Invalid enum constant"); // can never happen        
    }
  }

  public boolean isFinalState() {
    switch (this) {
      case FINISHED:
      case FAILED:
      case KILLED:
      case FRAMEWORK_FAILURE:
      case APP_MASTER_START_FAILED:
      case INITIALIZATION_FAILED:
        return true;
      default:
        return false;
    }
  }

}
