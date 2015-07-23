package se.kth.bbc.jobs;

import se.kth.bbc.jobs.execution.HopsJob;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

/**
 * Utility class for executing a HopsJob asynchronously. Passing the Hopsjob to
 * the method startExecution() will start the HopsJob asynchronously. The
 * HobsJob is supposed to take care of all aspects of execution, such as
 * creating a JobHistory object or processing output.
 * <p>
 * @author stig
 */
@Stateless
public class AsynchronousJobExecutor {

  @Asynchronous
  public void startExecution(HopsJob job) {
    job.runJob();
  }
}
