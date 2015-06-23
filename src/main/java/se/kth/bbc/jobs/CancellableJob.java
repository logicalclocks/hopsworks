package se.kth.bbc.jobs;

/**
 * Allows a job that has been submitted to be cancelled. The CancellableJob
 * is responsible of unregistering with the RunningJobTracker upon cancellation.
 * <p>
 * @author stig
 */
public interface CancellableJob {

  /**
   * Cancel the running job.
   * <p>
   * @throws Exception
   */
  public void cancelJob() throws Exception;
}
