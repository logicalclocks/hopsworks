package se.kth.bbc.jobs;

import java.util.Collection;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobInputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.bbc.project.Project;

/**
 * Class for containing the execution logic of a Hops job. Its internals
 * should take care of the entire job execution process. This entails creating
 * a persisted JobHistory object (in the init() method) that allows for
 * rerunning the same job, starting the actual job and possibly processing its
 * input/output and finally creating a new instance based on a previously run
 * job's JobHistory object.
 * <p>
 * @author stig
 */
public abstract class HopsJob {

  private JobHistoryFacade jobHistoryFacade;
  private boolean initialized = false;
  private JobHistory history;

  public HopsJob(JobHistoryFacade facade) {
    this.jobHistoryFacade = facade;
  }

  public final JobHistoryFacade getJobHistoryFacade() {
    return jobHistoryFacade;
  }

  public final void setJobHistoryFacade(JobHistoryFacade jobHistoryFacade) {
    this.jobHistoryFacade = jobHistoryFacade;
  }

  public final boolean isIdAssigned() {
    return initialized;
  }

  /**
   * Returns a copy of the current history object. Should not be used for
   * updating.
   * <p>
   * @return
   */
  public final JobHistory getHistory() {
    return new JobHistory(history);
  }

  protected final void updateState(JobState newState) {
    history = jobHistoryFacade.update(history, newState);
  }

  protected final void updateHistory(String name, JobState state,
          long executionDuration, String stdoutPath,
          String stderrPath, String appId, Collection<JobInputFile> inputFiles,
          Collection<JobOutputFile> outputFiles) {
    history = jobHistoryFacade.update(history, name, state,
            executionDuration, stdoutPath, stderrPath, appId, inputFiles,
            outputFiles);
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
   * Request a unique job id by creating a JobHistory object. Creates
   * and persists a JobHistory object that should ultimately enable this job to
   * be rerun. The object is in the state INITIALIZING. Upon success, returns
   * the created JobHistory object to allow tracking.
   * This method must be called before attempting to run the actual job.
   * <p>
   * @param config The configuration object with which this job is run.
   * @param userEmail The email of the user running the job.
   * @param project The project under which the job is being run.
   * @param jobType The type of job.
   * @return Unique id of the JobHistory object associated with this job.
   */
  public final JobHistory requestJobId(YarnJobConfiguration config,
          String userEmail,
          Project project,
          JobType jobType) {
    history = jobHistoryFacade.create(config.getAppName(), userEmail, project,
            jobType, JobState.INITIALIZING, null, null, null, null, config);
    initialized = true;
    return history;
  }

  /*
   * Methods meant to be overriden.
   */
  /**
   * Create a new job instance that runs the same job as contained in the
   * JobHistory object.
   * <p>
   * @param jh The JobHistory object whose execution to mimic.
   * @return The HopsJob object containing the job to be executed.
   * @throws IllegalArgumentException Thrown when the JobHistory object does not
   * describe an object ran by this class.
   */
  public abstract HopsJob getInstance(JobHistory jh) throws
          IllegalArgumentException;

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
