package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.user.model.Users;

/**
 * Orchestrates the execution of a Spark job: run job, update history
 * object.
 * <p>
 * @author stig
 */
public final class SparkJob extends YarnJob {

  private static final Logger logger = Logger.
          getLogger(SparkJob.class.getName());

  private final SparkJobConfiguration jobconfig; //Just for convenience

  /**
   *
   * @param job
   * @param user
   * @param services
   */
  public SparkJob(JobDescription job,
          Users user, AsynchronousJobExecutor services) {
    super(job, user, services);
    if (!(job.getJobConfig() instanceof SparkJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a SparkJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }
    this.jobconfig = (SparkJobConfiguration) job.getJobConfig();
  }

  @Override
  protected boolean setupJob() {
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    SparkYarnRunnerBuilder runnerbuilder = new SparkYarnRunnerBuilder(
            jobconfig.getJarPath(), jobconfig.getMainClass());
    runnerbuilder.setJobName(jobconfig.getAppName());
    String[] jobArgs = jobconfig.getArgs().trim().split(" ");
    runnerbuilder.addAllJobArgs(jobArgs);
    //Set spark runner options
    runnerbuilder.setExecutorCores(jobconfig.getExecutorCores());
    runnerbuilder.setExecutorMemory(jobconfig.getExecutorMemory());
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());

    //TODO: runnerbuilder.setExtraFiles(config.getExtraFiles());
    try {
      runner = runnerbuilder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      writeToLogs(new IOException("Failed to start Yarn client.", e));
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().
            getName())
            + Constants.SPARK_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stdout.log";
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().
            getName())
            + Constants.SPARK_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stderr.log";
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  @Override
  protected void cleanup() {
    //No special tasks to be done here.
  }

}
