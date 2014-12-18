package se.kth.bbc.jobs;

/**
 * Allows a job that has been submitted to be cancelled. The CancellableJob
 * is responsible of unregistering with the RunningJobTracker upon cancellation.
 * @author stig
 */
public interface CancellableJob {
  
  /**
   * Cancel the running job.
   * @throws Exception 
   */
  public void cancelJob() throws Exception;
}
