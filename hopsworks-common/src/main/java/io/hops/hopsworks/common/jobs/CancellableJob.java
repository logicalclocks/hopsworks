package io.hops.hopsworks.common.jobs;

/**
 * Allows a job that has been submitted to be cancelled. The CancellableJob
 * is responsible of unregistering with the RunningJobTracker upon cancellation.
 */
public interface CancellableJob {

  /**
   * Cancel the running job.
   * <p/>
   * @throws Exception
   */
  public void cancelJob() throws Exception;
}
