package io.hops.hopsworks.common.jobs.tensorflow;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class TensorFlowJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(TensorFlowJob.class.getName());
  private final String tfUser;
  protected TensorFlowYarnRunnerBuilder runnerbuilder;

  public TensorFlowJob(JobDescription job, AsynchronousJobExecutor services, Users user, final String hadoopDir,
      String tfUser, String jobUser, YarnJobsMonitor jobsMonitor) {
    super(job, services, user, jobUser, hadoopDir, jobsMonitor);
    if (!(job.getJobConfig() instanceof TensorFlowJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a TensorFlowJobConfiguration object. Received: " + 
              job.getJobConfig().getClass());
    }
    this.tfUser = tfUser;
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    super.setupJob(dfso);
    TensorFlowJobConfiguration jobconfig = (TensorFlowJobConfiguration) jobDescription.getJobConfig();

    runnerbuilder = new TensorFlowYarnRunnerBuilder(jobDescription);
    runnerbuilder.setJobName(jobconfig.getAppName());
    //Check if the user provided application arguments
    if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
      String[] jobArgs = jobconfig.getArgs().trim().split(" ");
      runnerbuilder.addAllJobArgs(jobArgs);
    }
    runnerbuilder.setJobName(jobconfig.getAppName());
    //Set spark runner options
    runnerbuilder.setNumOfPs(jobconfig.getNumOfPs());
    runnerbuilder.setNumOfWorkers(jobconfig.getNumOfWorkers());
    runnerbuilder.setWorkerMemory(jobconfig.getWorkerMemory());
    runnerbuilder.setWorkerVCores(jobconfig.getWorkerVCores());
    runnerbuilder.setWorkerGPUs(jobconfig.getNumOfGPUs());

    //Set Yarn running options
    runnerbuilder.setAmMemory(jobconfig.getAmMemory());
    runnerbuilder.setAmVCores(jobconfig.getAmVCores());
    runnerbuilder.setQueue(jobconfig.getAmQueue());

    runnerbuilder.setServiceProps(serviceProps);
    runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    runnerbuilder.addExtraFiles(projectLocalResources);

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().getName())
        + Settings.TENSORFLOW_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().getName())
        + Settings.TENSORFLOW_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);

    try {
      runner = runnerbuilder.getYarnRunner(jobDescription.getProject().getName(), tfUser, jobUser, hadoopDir,
          services);

    } catch (Exception e) {
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
    //Stop flink cluster first
    try {
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(this.hadoopDir + "/bin/yarn application -kill "
          + appid);
    } catch (IOException ex1) {
      LOG.log(Level.SEVERE, "Unable to stop flink cluster with appID:"
          + appid, ex1);
    }
  }

}
