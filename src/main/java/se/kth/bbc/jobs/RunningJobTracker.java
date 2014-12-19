package se.kth.bbc.jobs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.ejb.Singleton;

/**
 * Singleton bean for keeping track of running jobs in the system. For
 * each running job, its CancellableJob object is registered. The
 * RunningJobTracker
 * keeps track of a job until its unregistration. A job can also be cancelled
 * through this bean.
 * <p>
 * @author stig
 */
@Singleton
public class RunningJobTracker {

  //TODO: make a job cancellable only by its owner or by the owner of the study it was started in. 
  //Perhaps this should be implemented in the CancellableJob.cancelJob() method though. Or the interface should be changed.
  //TODO: impose restrictions on who can register and unregister a job?
  //TODO: change visibility of class to package? --> no unwanted unregistration of jobs
  private static final String MSG_JOB_ALREADY_PRESENT
          = "A job with this ID has already been registered.";
  private static final String MSG_JOB_NOT_RUNNING
          = "Attempting to unregister job with non-existing id.";

  private final ConcurrentMap<Long, CancellableJob> runningJobs
          = new ConcurrentHashMap<>();

  //TODO: use different exception or return value?
  public void registerJob(Long jobId, CancellableJob job) throws
          IllegalStateException {
    CancellableJob previous = runningJobs.putIfAbsent(jobId, job);
    if (previous != null) {
      throw new IllegalStateException(MSG_JOB_ALREADY_PRESENT);
    }
  }

  //TODO: use different exception or return value?
  public void unregisterJob(Long jobId) throws IllegalStateException {
    CancellableJob previous = runningJobs.remove(jobId);
    if (previous == null) {
      throw new IllegalStateException(MSG_JOB_NOT_RUNNING);
    }
  }

  public boolean isJobRunning(Long jobId) {
    if (jobId != null) {
      return runningJobs.containsKey(jobId);
    } else {
      return false;
    }
  }

  public CancellableJob getCancellableJob(Long id) {
    return runningJobs.get(id);
  }

}
