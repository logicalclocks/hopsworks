package se.kth.bbc.jobs.jobhistory;

import java.util.EnumSet;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;

/**
 *
 * @author stig
 */
public enum JobState {

  INITIALIZING("Initializing"),
  INITIALIZATION_FAILED("Initialization failed"),
  FINISHED("Finished"),
  RUNNING("Running"),
  ACCEPTED("Accepted"),
  FAILED("Failed"),
  KILLED("Killed"),
  NEW("New"),
  NEW_SAVING("New, saving"),
  SUBMITTED("Submitted"),
  FRAMEWORK_FAILURE("Framework failure"),
  STARTING_APP_MASTER("Starting Application Master"),
  APP_MASTER_START_FAILED("Failed starting AM");

  private final String readable;

  private JobState(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
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

  public static EnumSet<JobState> getRunningStates() {
    return EnumSet.of(INITIALIZING, RUNNING, ACCEPTED, NEW, NEW_SAVING,
            SUBMITTED, STARTING_APP_MASTER);
  }

}
