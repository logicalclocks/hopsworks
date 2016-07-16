package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
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
  private final String sparkUser; //must be glassfish

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
   * @param kafkaAddress
   */
  public SparkJob(JobDescription job, AsynchronousJobExecutor services,
      Users user, final String hadoopDir,
      final String sparkDir, final String nameNodeIpPort, String sparkUser,
      String jobUser, String kafkaAddress) {
    super(job, services, user, jobUser, hadoopDir, nameNodeIpPort, kafkaAddress);
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
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    super.setupJob(dfso);
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    SparkYarnRunnerBuilder runnerbuilder = new SparkYarnRunnerBuilder(
        jobconfig.getJarPath(), jobconfig.getMainClass());
    runnerbuilder.setJobName(jobconfig.getAppName());
    //Check if the user provided application arguments
    if(jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()){
            String[] jobArgs = jobconfig.getArgs().trim().split(" ");
            runnerbuilder.addAllJobArgs(jobArgs);
    }  
    //Set spark runner options
    runnerbuilder.setExecutorCores(jobconfig.getExecutorCores());
    runnerbuilder.setExecutorMemory("" + jobconfig.getExecutorMemory() + "m");
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    if(jobconfig.isDynamicExecutors()){
      runnerbuilder.setDynamicExecutors(jobconfig.isDynamicExecutors());
      runnerbuilder.setNumberOfExecutorsMin(jobconfig.getSelectedMinExecutors());
      runnerbuilder.setNumberOfExecutorsMax(jobconfig.getSelectedMaxExecutors());
      runnerbuilder.setNumberOfExecutorsInit(jobconfig.getNumberOfExecutorsInit());
    }
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());
    runnerbuilder.setSparkHistoryServerIp(jobconfig.getHistoryServerIp());

    runnerbuilder.setSessionId(jobconfig.getSessionId());
    runnerbuilder.setKafkaAddress(kafkaAddress);
    
    runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources
    runnerbuilder.addExtraFiles(projectLocalResources);
    if(jobSystemProperties != null && !jobSystemProperties.isEmpty()){
      for(Entry<String,String> jobSystemProperty: jobSystemProperties.entrySet()){
        runnerbuilder.addSystemProperty(jobSystemProperty.getKey(), jobSystemProperty.getValue());
      }
    }

    try {
      runner = runnerbuilder.
          getYarnRunner(jobDescription.getProject().getName(),
              sparkUser, jobUser, hadoopDir, sparkDir, nameNodeIpPort);

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
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
  }

}
