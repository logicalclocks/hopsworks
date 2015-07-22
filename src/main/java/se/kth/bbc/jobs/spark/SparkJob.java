package se.kth.bbc.jobs.spark;

import java.util.logging.Logger;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.jobs.yarn.YarnRunner;

/**
 * Orchestrates the execution of a Spark job: run job, update history
 * object.
 * <p>
 * @author stig
 */
public final class SparkJob extends YarnJob {

  private static final Logger logger = Logger.
          getLogger(SparkJob.class.getName());

  public SparkJob(ExecutionFacade facade, YarnRunner runner,
          FileOperations fops) {
    super(facade, runner, fops);
  }

  @Override
  protected void runJobInternal() {
    //Keep track of time and start job
    long startTime = System.currentTimeMillis();
    //Try to start the AM
    boolean proceed = super.startJob();
    //If success: monitor running job
    if (!proceed) {
      return;
    }
    proceed = super.monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    super.copyLogs();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    updateExecution(getFinalState(), duration, null, null, null, null, null);
  }

}
