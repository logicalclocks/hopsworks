package se.kth.bbc.jobs.execution;

import java.util.Collection;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.jobhistory.JobInputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.hopsworks.user.model.Users;

/**
 * Contains the execution logic of a Hops job. Its internals
 * should take care of the entire job execution process. This entails creating
 * a persisted JobHistory object (in the init() method) that allows for
 * rerunning the same job, starting the actual job and possibly processing its
 * input/output and finally creating a new instance based on a previously run
 * job's JobHistory object.
 * <p>
 * @author stig
 */
public abstract class HopsJob {

  private ExecutionFacade executionFacade;
  private boolean initialized = false;
  private Execution execution;

  public HopsJob(ExecutionFacade executionFacade) {
    this.executionFacade = executionFacade;
  }

  public ExecutionFacade getExecutionFacade() {
    return executionFacade;
  }

  public void setExecutionFacade(ExecutionFacade executionFacade) {
    this.executionFacade = executionFacade;
  }

  public final boolean isIdAssigned() {
    return initialized;
  }

  /**
   * Returns a copy of the current execution object. The copy is not persisted
   * and should not be used for updating the database.
   * <p>
   * @return
   */
  public final Execution getExecution() {
    return new Execution(execution);
  }

  protected final void updateState(JobState newState) {
    execution = executionFacade.updateState(execution, newState);
  }

  protected final void updateExecution(JobState state,
          long executionDuration, String stdoutPath,
          String stderrPath, String appId, Collection<JobInputFile> inputFiles,
          Collection<JobOutputFile> outputFiles) {
    Execution upd = executionFacade.updateAppId(execution, appId);
    upd = executionFacade.updateExecutionTime(upd, executionDuration);
    upd = executionFacade.updateOutput(upd, outputFiles);
    upd = executionFacade.updateState(upd, state);
    upd = executionFacade.updateStdErrPath(upd, stderrPath);
    upd = executionFacade.updateStdOutPath(upd, stdoutPath);
    this.execution = upd;
  }

  /**
   * Takes care of the execution of the job. First checks if the job has
   * been assigned a job id (achieved by calling requestJobId) and then calls
   * runJobInternal(), which takes care of the real execution of the job.
   * Is not intended to perform any work asynchronously, so should not be called
   * from a synchronous context.
   * <p>
   * @throws IllegalStateException Thrown when called on a job that has not been
   * assigned a job id.
   */
  public final void runJob() throws IllegalStateException {
    if (!initialized) {
      throw new IllegalStateException("Trying to run a job for which no id has"
              + "been assigned yet.");
    }
    runJobInternal();
  }

  /**
   * Request a unique execution id by creating an Execution object. Creates
   * and persists an Execution object. The object is in the state INITIALIZING.
   * Upon success, returns the created Execution object to allow tracking.
   * This method must be called before attempting to run the actual execution.
   * <p>
   * @param j The job for which an execution should be started.
   * @param user The user who is starting this job.
   * @return Unique id of the JobHistory object associated with this job.
   */
  public final Execution requestJobId(JobDescription j, Users user) {
    execution = executionFacade.create(j, user, null, null, null);
    initialized = (execution.getId() != null);
    return execution;
  }

  /**
   * Takes care of the execution of the job. Called by runJob() which first
   * checks whether the job has been initialized. In the execution, the job
   * should
   * take care of updating its JobHistory object, running the job and possibly
   * process input or output.
   * <p>
   * Intended to be called from an asynchronous context so should not create
   * threads ad libidum.
   * <p>
   * Note that updating the JobHistory object correctly and persistently is
   * crucial, for this object is used to check the status of (running) jobs.
   */
  protected abstract void runJobInternal();
}
