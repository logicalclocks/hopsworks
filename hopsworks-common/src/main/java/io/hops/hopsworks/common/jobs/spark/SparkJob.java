package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.util.Settings;

/**
 * Orchestrates the execution of a Spark job: run job, update history object.
 * <p>
 */
public class SparkJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(SparkJob.class.getName());
  private final String sparkDir;
  private final String sparkUser;
  protected SparkYarnRunnerBuilder runnerbuilder;

  /**
   *
   * @param job
   * @param user
   * @param services
   * @param hadoopDir
   * @param sparkDir
   * @param nameNodeIpPort
   * @param sparkUser
   * @param jobUser
   */
  public SparkJob(JobDescription job, AsynchronousJobExecutor services,
      Users user, final String hadoopDir,
      final String sparkDir, final String nameNodeIpPort, String sparkUser,
      String jobUser) {
    super(job, services, user, jobUser, hadoopDir, nameNodeIpPort);
    if (!(job.getJobConfig() instanceof SparkJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a SparkJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
    this.sparkDir = sparkDir;
    this.sparkUser = sparkUser;
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    super.setupJob(dfso);
    SparkJobConfiguration jobconfig = (SparkJobConfiguration) jobDescription.getJobConfig();
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    //If runnerbuilder is not null, it has been instantiated by child class,
    //i.e. AdamJob
    if (runnerbuilder == null) {
      runnerbuilder = new SparkYarnRunnerBuilder(jobDescription);
      runnerbuilder.setJobName(jobconfig.getAppName());
      //Check if the user provided application arguments
      if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
        String[] jobArgs = jobconfig.getArgs().trim().split(" ");
        runnerbuilder.addAllJobArgs(jobArgs);
      }
    }

    //Set spark runner options
    runnerbuilder.setExecutorCores(jobconfig.getExecutorCores());
    runnerbuilder.setExecutorMemory("" + jobconfig.getExecutorMemory() + "m");
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    if (jobconfig.isDynamicExecutors()) {
      runnerbuilder.setDynamicExecutors(jobconfig.isDynamicExecutors());
      runnerbuilder.setNumberOfExecutorsMin(jobconfig.getSelectedMinExecutors());
      runnerbuilder.setNumberOfExecutorsMax(jobconfig.getSelectedMaxExecutors());
      runnerbuilder.setNumberOfExecutorsInit(jobconfig.
          getNumberOfExecutorsInit());
    }
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());

    //Set Kafka params
    runnerbuilder.setServiceProps(serviceProps);
    runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    runnerbuilder.addExtraFiles(projectLocalResources);
    if (jobSystemProperties != null && !jobSystemProperties.isEmpty()) {
      for (Entry<String, String> jobSystemProperty : jobSystemProperties.
          entrySet()) {
        runnerbuilder.addSystemProperty(jobSystemProperty.getKey(),
            jobSystemProperty.getValue());
      }
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(hadoopDir,
        jobDescription.
            getProject().
            getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(hadoopDir,
        jobDescription.
            getProject().
            getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);

    try {
      runner = runnerbuilder.
          getYarnRunner(jobDescription.getProject().getName(),
              sparkUser, jobUser, hadoopDir, sparkDir, nameNodeIpPort);

    } catch (IOException e) {
      LOG.log(Level.WARNING,
          "Failed to create YarnRunner.", e);
      try {
        writeToLogs(e.getLocalizedMessage());
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", ex);
      }
      return false;
    }

    return true;
  }

  @Override
  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
  }

  @Override
  protected void stopJob(String appid) {
    super.stopJob(appid);
  }

}
