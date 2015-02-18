package se.kth.bbc.jobs;

import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;

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
  private Long jobId;

  public HopsJob(JobHistoryFacade facade) {
    this.jobHistoryFacade = facade;
  }

  public final JobHistoryFacade getJobHistoryFacade() {
    return jobHistoryFacade;
  }

  public final void setJobHistoryFacade(JobHistoryFacade jobHistoryFacade) {
    this.jobHistoryFacade = jobHistoryFacade;
  }
  
  public final boolean isIdAssigned(){
    return initialized;
  }
  
  public final Long getJobId(){
    return jobId;
  }
  
  protected final void updateState(JobState newState){
    jobHistoryFacade.update(jobId, newState);
  }  
    
  protected final void updateArgs(String args){
    jobHistoryFacade.updateArgs(jobId, args);
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
   * be rerun. The object is in the state INITIALIZING. Upon success, returns the 
   * unique id of the created JobHistory object to allow tracking. 
   * This method must be called before attempting to run it.
   * <p>
   * @param jobname The (optional) name for the job.
   * @param userEmail The email of the user running the job.
   * @param studyname The study under which the job is being run.
   * @param jobType The type of job.
   * @return Unique id of the JobHistory object associated with this job.
   * @throws JobInitializationFailedException Thrown when initialization failed.
   */
  public final Long requestJobId(String jobname, String userEmail, String studyname,
          JobType jobType){
    jobId = jobHistoryFacade.create(jobname, userEmail, studyname, jobType,
            null, JobState.INITIALIZING, null, null, null, null);
    initialized = true;
    return jobId;
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
