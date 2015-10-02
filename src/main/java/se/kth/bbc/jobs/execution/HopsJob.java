package se.kth.bbc.jobs.execution;

import java.util.Collection;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobInputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.user.model.Users;

/**
 * Contains the execution logic of a Hops job. This class takes care of the main
 * flow control of the application. The job is started by calling the execute()
 * method. Note that this method is blocking. Before this, however, the client
 * must have requested an Execution id by calling the appropriate method on
 * this class. HopsJob then sets up and runs the actual job and finally
 * allows for some cleanup. This class takes care of execution time tracking as
 * well.
 * <p/>
 * Three abstract methods are provided for overriding:
 * <ul>
 * <li>setupJob() - This method is called after an Execution id has been
 * procured. </li>
 * <li>runJob() - This method is called after setupJob() finishes. </li>
 * <li>cleanup() - This method is called after runJob() finishes. </li>
 * </ul>
 * <p/>
 * The calls to each of these methods are blocking.
 * <p/>
 * @author stig
 */
public abstract class HopsJob {

  private Execution execution;
  private boolean initialized = false;

  //Service provider providing access to facades
  protected final AsynchronousJobExecutor services;
  protected final JobDescription jobDescription;
  private final Users user;

  /**
   * Create a HopsJob instance.
   * <p/>
   * @param jobDescription The JobDescription to be executed.
   * @param services A service provider giving access to several execution
   * services.
   * @param user The user executing this job.
   * @throws NullPointerException If either of the given arguments is null.
   */
  protected HopsJob(JobDescription jobDescription,
          AsynchronousJobExecutor services, Users user) throws
          NullPointerException {
    //Check validity
    if (jobDescription == null) {
      throw new NullPointerException("Cannot run a null JobDescription.");
    } else if (services == null) {
      throw new NullPointerException("Cannot run without a service provider.");
    } else if (user == null) {
      throw new NullPointerException("A job cannot be run by a null user!");
    }
    //We can safely proceed
    this.jobDescription = jobDescription;
    this.services = services;
    this.user = user;
  }

  /**
   * Returns a copy of the current execution object. The copy is not persisted
   * and should not be used for updating the database.
   * <p/>
   * @return
   */
  public final Execution getExecution() {
    return new Execution(execution);
  }

  /**
   * Update the current state of the Execution entity to the given state.
   * <p/>
   * @param newState
   */
  protected final void updateState(JobState newState) {
    execution = services.getExecutionFacade().updateState(execution, newState);
  }

  /**
   * Update the current Execution entity with the given values.
   * <p/>
   * @param state
   * @param executionDuration
   * @param stdoutPath
   * @param stderrPath
   * @param appId
   * @param inputFiles
   * @param outputFiles
   */
  protected final void updateExecution(JobState state,
          long executionDuration, String stdoutPath,
          String stderrPath, String appId, Collection<JobInputFile> inputFiles,
          Collection<JobOutputFile> outputFiles) {
    Execution upd = services.getExecutionFacade().updateAppId(execution, appId);
    upd = services.getExecutionFacade().updateExecutionTime(upd,
            executionDuration);
    upd = services.getExecutionFacade().updateOutput(upd, outputFiles);
    upd = services.getExecutionFacade().updateState(upd, state);
    upd = services.getExecutionFacade().updateStdErrPath(upd, stderrPath);
    upd = services.getExecutionFacade().updateStdOutPath(upd, stdoutPath);
    this.execution = upd;
  }

  /**
   * Execute the job and keep track of its execution time. The execution flow is
   * outlined in the class documentation. Internally, this method calls
   * setupJob(), runJob() and cleanup() in that order.
   * This method is blocking.
   * <p/>
   * @throws IllegalStateException If no Execution id has been requested yet.
   */
  public final void execute() throws IllegalStateException {
    if (!initialized) {
      throw new IllegalStateException(
              "Cannot execute before acquiring an Execution id.");
    }
    long starttime = System.currentTimeMillis();
    boolean proceed = setupJob();
    if (!proceed) {
      long executiontime = System.currentTimeMillis() - starttime;
      updateExecution(JobState.INITIALIZATION_FAILED, executiontime, null, null,
              null, null, null);
      cleanup();
      return;
    } else {
      updateState(JobState.STARTING_APP_MASTER);
    }
    runJob();
    long executiontime = System.currentTimeMillis() - starttime;
    updateExecution(null, executiontime, null, null, null, null, null);
    cleanup();
  }

  /**
   * Called before runJob, should setup the job environment to allow it to be
   * run.
   * <p/>
   * @return False if execution should be aborted. Cleanup() is still executed
   * in that case.
   */
  protected abstract boolean setupJob();

  /**
   * Takes care of the execution of the job. Called by execute() after
   * setupJob(), if that method indicated to be successful.
   * Note that this method should update the Execution object correctly and
   * persistently, since this object is used to check the status of (running)
   * jobs.
   */
  protected abstract void runJob();

  /**
   * Called after runJob() completes, allows the job to perform some cleanup, if
   * necessary.
   */
  protected abstract void cleanup();

  /**
   * Request a unique execution id by creating an Execution object. Creates
   * and persists an Execution object. The object is in the state INITIALIZING.
   * Upon success, returns the created Execution object to allow tracking.
   * This method must be called before attempting to run the actual execution.
   * <p/>
   * @return Unique Execution object associated with this job.
   */
  public final Execution requestExecutionId() {
    execution = services.getExecutionFacade().create(jobDescription, user, null,
            null, null);
    initialized = (execution.getId() != null);
    return execution;
  }

  /**
   * Check whether the HopsJob was initialized correctly, {@literal i.e.} if an
   * Execution id has been acquired.
   * <p/>
   * @return
   */
  public final boolean isInitialized() {
    return initialized;
  }

  /**
   * Write a message to the application logs.
   * <p/>
   * @param message
   */
  protected final void writeToLogs(String message) {
    //TODO: implement this.
  }

  /**
   * Write an Exception message to the application logs.
   * <p/>
   * @param e
   */
  protected final void writeToLogs(Exception e) {
    writeToLogs(e.getLocalizedMessage());
  }
}
