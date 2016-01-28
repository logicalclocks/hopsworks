package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 * Orchestrates the execution of a Spark job: run job, update history object.
 * <p/>
 * @author stig
 */
public final class SparkJob extends YarnJob {

  private static final Logger logger = Logger.
          getLogger(SparkJob.class.getName());

  private final SparkJobConfiguration jobconfig; //Just for convenience
  private final String sparkDir;
  private final String sparkUser;

  /**
   *
   * @param job
   * @param user
   * @param services
   * @param hadoopDir
   * @param sparkDir
   * @param sparkUser
   */
  public SparkJob(JobDescription job, AsynchronousJobExecutor services,
          Users user, final String hadoopDir,
          final String sparkDir, String sparkUser) {
    super(job, services, user, hadoopDir);
    if (!(job.getJobConfig() instanceof SparkJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a SparkJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }
    this.jobconfig = (SparkJobConfiguration) job.getJobConfig();
    this.sparkDir = sparkDir;
    this.sparkUser = sparkUser;
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
    runnerbuilder.setExecutorMemory("" + jobconfig.getExecutorMemory() + "m");
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());

    //TODO: runnerbuilder.setExtraFiles(config.getExtraFiles());
    try {
      runner = runnerbuilder.
              getYarnRunner(jobDescription.getProject().getName(),
                      sparkUser, hadoopDir, sparkDir);

    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      writeToLogs(new IOException("Failed to start Yarn client.", e));
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(hadoopDir,
            jobDescription.
            getProject().
            getName())
            + Settings.SPARK_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stdout.log";
    String stdErrFinalDestination = Utils.getHdfsRootPath(hadoopDir,
            jobDescription.
            getProject().
            getName())
            + Settings.SPARK_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stderr.log";
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  @Override
  protected void cleanup() {
    logger.log(Level.INFO, "Job finished performing cleanup...");
    monitor.close();
    monitor = null;
  }

}
