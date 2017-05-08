package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.Settings;

public abstract class YarnJob extends HopsJob {

  private static final Logger LOG = Logger.getLogger(YarnJob.class.getName());

  private static final int DEFAULT_MAX_STATE_POLL_RETRIES = 10;
  private static final int DEFAULT_POLL_TIMEOUT_INTERVAL = 1; //in seconds

  protected YarnRunner runner;

  protected YarnMonitor monitor = null;
  private final Configuration conf = new Configuration();

  private String stdOutFinalDestination, stdErrFinalDestination;
  private boolean started = false;
  private boolean removedFiles = false;
  private JobState finalState = null;
  protected List<LocalResourceDTO> projectLocalResources;
  protected Map<String, String> jobSystemProperties;

  protected final String jobUser;

  /**
   * Constructor for job interacting with the Kafka service.
   *
   * @param job
   * @param user
   * @param services
   * @param jobUser
   * @param hadoopDir
   * @param nameNodeIpPort
   * @throws IllegalArgumentException If the JobDescription does not contain a
   * YarnJobConfiguration object.
   */
  public YarnJob(JobDescription job, AsynchronousJobExecutor services,
      Users user, String jobUser, String hadoopDir, String nameNodeIpPort) {
    super(job, services, user, hadoopDir, nameNodeIpPort);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must be a YarnJobConfiguration object. Received class: "
          + job.getJobConfig().getClass());
    }
    LOG.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
    this.jobSystemProperties = new HashMap<>();
    this.projectLocalResources = new ArrayList<>();
    this.jobUser = jobUser;
  }

  public final void setStdOutFinalDestination(String stdOutFinalDestination) {
    this.stdOutFinalDestination = stdOutFinalDestination;
  }

  public final void setStdErrFinalDestination(String stdErrFinalDestination) {
    this.stdErrFinalDestination = stdErrFinalDestination;
  }

  protected final String getStdOutFinalDestination() {
    return this.stdOutFinalDestination;
  }

  protected final String getStdErrFinalDestination() {
    return this.stdErrFinalDestination;
  }

  protected final boolean appFinishedSuccessfully() {
    return finalState == JobState.FINISHED;
  }

  protected final JobState getFinalState() {
    if (finalState == null) {
      finalState = JobState.FAILED;
    }
    return finalState;
  }

  /**
   * Start the YARN application master.
   * 
   * @param udfso
   * @param dfso
   * @return True if the AM was started, false otherwise.
   * @throws IllegalStateException If the YarnRunner has not been set yet.
   */
  protected final boolean startApplicationMaster(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso) throws IllegalStateException {
    if (runner == null) {
      throw new IllegalStateException(
          "The YarnRunner has not been initialized yet.");
    }
    try {
      updateState(JobState.STARTING_APP_MASTER);
      monitor = runner.startAppMaster(services, jobDescription.getProject(),
          dfso, user.getUsername());
      started = true;
      updateExecution(null, -1, null, null, monitor.getApplicationId().
          toString(), null, null, null, 0);
      return true;
    } catch (AccessControlException ex) {
      LOG.log(Level.SEVERE, "Permission denied:- {0}", ex.getMessage());
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } catch (YarnException | IOException | URISyntaxException e) {
      LOG.log(Level.SEVERE,
          "Failed to start application master for execution "
          + getExecution()
          + ". Aborting execution",
          e);
      writeLog("Failed to start application master for execution "
          + getExecution()
          + ". Aborting execution",
          e, udfso);
      try {
        runner.removeAllNecessary();
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Failed to remove files for failed execution {0}", getExecution());
        writeLog("Failed to remove files for failed execution " + getExecution(), ex, udfso);
      }
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    //Check if this job is using Kakfa, and include certificate
    //in local resources
    serviceProps = new ServiceProperties(services.getSettings().getHopsworksMasterPasswordSsl(),
        services.getSettings().getHopsworksMasterPasswordSsl(), jobDescription.getProject().getId(),
        jobDescription.getProject().getName(), services.getSettings().getRestEndpoint(), jobDescription.getName(),
        new ElasticProperties(services.getSettings().getElasticRESTEndpoint()));

    if (jobDescription.getProject().getConda()) {
      serviceProps.initAnaconda(services.getSettings().getAnacondaProjectDir(jobDescription.getProject().getName())
          + File.separator + "bin" + File.separator + "python");
    }
    Collection<ProjectServices> projectServices = jobDescription.getProject().
        getProjectServicesCollection();
    if (projectServices != null && !projectServices.isEmpty()) {
      Iterator<ProjectServices> iter = projectServices.iterator();
      while (iter.hasNext()) {
        ProjectServices projectService = iter.next();
        //If the project is of type KAFKA
        if (projectService.getProjectServicesPK().getService()
            == ProjectServiceEnum.KAFKA && (jobDescription.getJobType()
            == JobType.FLINK || jobDescription.getJobType() == JobType.SPARK)
            && jobDescription.getJobConfig() instanceof YarnJobConfiguration
            && jobDescription.getJobConfig().getKafka() != null) {
          serviceProps.initKafka();
          //Set Kafka specific properties to serviceProps
          serviceProps.getKafka().setBrokerAddresses(services.getSettings().
              getKafkaConnectStr());
          serviceProps.getKafka().setRestEndpoint(services.getSettings().
              getRestEndpoint());
          serviceProps.getKafka().setTopics(jobDescription.getJobConfig().
              getKafka().getTopics());
          serviceProps.getKafka().setProjectConsumerGroups(jobDescription.
              getProject().getName(), jobDescription.
                  getJobConfig().getKafka().getConsumergroups());
          return true;
        }
      }
    }
    return true;
  }

  final EnumSet<YarnApplicationState> finalAppState = EnumSet.of(
      YarnApplicationState.FINISHED, YarnApplicationState.FAILED,
      YarnApplicationState.KILLED);

  /**
   * Monitor the state of the job.
   * 
   * @return True if monitoring succeeded all the way, false if failed in
   * between.
   */
  protected final boolean monitor() {
    try (YarnMonitor r = monitor.start()) {
      if (!started) {
        throw new IllegalStateException(
            "Trying to monitor a job that has not been started!");
      }
      YarnApplicationState appState;
      FinalApplicationStatus finalAppStatus;
      float progress;
      int failures;
      try {
        appState = r.getApplicationState();
        finalAppStatus = r.getFinalApplicationStatus();
        progress = r.getProgress();
        updateProgress(progress);
        updateState(JobState.getJobState(appState));
        updateFinalStatus(JobFinalStatus.getJobFinalStatus(finalAppStatus));
        //count how many consecutive times the state could not be polled. Cancel if too much.
        failures = 0;
      } catch (YarnException | IOException ex) {
        LOG.log(Level.WARNING,
            "Failed to get application state for execution"
            + getExecution(), ex);
        appState = null;
        failures = 1;
      }

      //Loop as long as the application is in a running/runnable state
      while (appState != YarnApplicationState.FAILED && appState
          != YarnApplicationState.FINISHED && appState
          != YarnApplicationState.KILLED && failures
          <= DEFAULT_MAX_STATE_POLL_RETRIES) {
        //wait to poll another time
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime)
            < DEFAULT_POLL_TIMEOUT_INTERVAL * 1000) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            //not much...
          }
        }

        try {
          appState = r.getApplicationState();
          finalAppStatus = r.getFinalApplicationStatus();
          progress = r.getProgress();
          updateProgress(progress);
          updateState(JobState.getJobState(appState));
          updateFinalStatus(JobFinalStatus.getJobFinalStatus(finalAppStatus));
          failures = 0;
        } catch (YarnException | IOException ex) {
          failures++;
          LOG.log(Level.WARNING,
              "Failed to get application state for execution "
              + getExecution() + ". Tried " + failures + " time(s).", ex);
        }
        //Remove local and hdfs files (localresources)this job uses
        if (!removedFiles && finalAppState.contains(appState)) {
          try {
            runner.removeAllNecessary();
            removedFiles = true;
          } catch (IOException ex) {
            LOG.log(Level.SEVERE,
                "Exception while trying to delete job tmp files "
                + getExecution(), ex);
          }
        }
      }

      if (failures > DEFAULT_MAX_STATE_POLL_RETRIES) {
        try {
          LOG.log(Level.SEVERE, "Killing application, {0}, because unable to poll for status.", getExecution());
          r.cancelJob(r.getApplicationId().toString());
          updateState(JobState.KILLED);
          updateFinalStatus(JobFinalStatus.KILLED);
          updateProgress(0);
          finalState = JobState.KILLED;
          runner.removeAllNecessary();
        } catch (YarnException | IOException ex) {
          LOG.log(Level.SEVERE,
              "Failed to cancel execution, " + getExecution()
              + " after failing to poll for status.", ex);
          updateState(JobState.FRAMEWORK_FAILURE);
          finalState = JobState.FRAMEWORK_FAILURE;
        }
        return false;
      }
      finalState = JobState.getJobState(appState);
      return true;
    }
  }
  
  /**
   * Removes the marker file for streaming jobs if it exists, after a non FINISHED/SUCCEEDED job.
   * @param udfso 
   */
  private void removeMarkerFile(DistributedFileSystemOps udfso) {
    String marker = Settings.getJobMarkerFile(jobDescription, monitor.getApplicationId().toString());
    try {
      if (udfso.exists(marker)) {
        udfso.rm(new org.apache.hadoop.fs.Path(marker), false);
      }
    } catch (IOException ex) {
      LOG.log(Level.WARNING, "Could not remove marker file for job:{0}, with appId:{1}, {2}", new Object[]{
        jobDescription.getName(),
        monitor.getApplicationId().
        toString(), ex.getMessage()});
    }
  }

  
  /**
   * Copy the AM logs to their final destination.
   *
   * @param udfso
   */
  protected void copyLogs(DistributedFileSystemOps udfso) {
    try {
      if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
        stdOutFinalDestination = stdOutFinalDestination + getExecution().
            getAppId()
            + File.separator + "stdout.log";
        if (!runner.areLogPathsHdfs() && !runner.areLogPathsAggregated()) {
          udfso.copyToHDFSFromLocal(true, runner.
              getStdOutPath(),
              stdOutFinalDestination);
        } else if (runner.areLogPathsAggregated()) {
          String[] desiredLogTypes = {"out"};
          YarnLogUtil.copyAggregatedYarnLogs(
              udfso, runner.
                  getStdOutPath(),
              stdOutFinalDestination, desiredLogTypes);

        } else {
          udfso.renameInHdfs(
              runner.
                  getStdOutPath(),
              stdOutFinalDestination);
        }
      }
      if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
        stdErrFinalDestination = stdErrFinalDestination + getExecution().
            getAppId()
            + File.separator + "stderr.log";
        if (!runner.areLogPathsHdfs() && !runner.areLogPathsAggregated()) {
          udfso.copyToHDFSFromLocal(true, runner.
              getStdErrPath(),
              stdErrFinalDestination);
        } else if (runner.areLogPathsAggregated()) {
          String[] desiredLogTypes = {"err", ".log"};
          YarnLogUtil.copyAggregatedYarnLogs(
              udfso, runner.
                  getStdOutPath(),
              stdErrFinalDestination, desiredLogTypes);
        } else {
          udfso.renameInHdfs(
              runner.
                  getStdErrPath(),
              stdErrFinalDestination);
        }
      }
      updateExecution(null, -1, stdOutFinalDestination, stdErrFinalDestination,
          null, null, null, null, 0);
    } catch (IOException e) {
      LOG.log(Level.SEVERE,
          "Exception while trying to write logs for execution "
          + getExecution() + " to HDFS.", e);
    }
  }

  protected void writeLog(String message, Exception exception, DistributedFileSystemOps udfso) {

    Date date = new Date();
    String dateString = date.toString();
    dateString = dateString.replace(" ", "_").replace(":", "-");
    stdErrFinalDestination = stdErrFinalDestination + jobDescription.getName() + dateString + "/stderr.log";
    YarnLogUtil.writeLog(udfso, stdErrFinalDestination, message, exception);
    updateExecution(null, -1, null, stdErrFinalDestination,
        null, null, null, null, 0);
  }

  @Override
  protected void writeToLogs(String message, Exception e) throws IOException {
    writeLog(message, e, services.getFileOperations(jobUser));
  }

  @Override
  protected void writeToLogs(String message) throws IOException {
    writeToLogs(message, null);
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso) {
    // Try to start the AM
    boolean proceed = startApplicationMaster(udfso, dfso);

    if (!proceed) {
      return;
    }
    proceed = monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    updateState(JobState.AGGREGATING_LOGS);
    copyLogs(udfso);
    updateState(getFinalState());
    removeMarkerFile(udfso);
    
  }

  @Override
  //DOESN'T WORK FOR NOW
  protected void stopJob(String appid) {
    YarnClient yarnClient = null;
    try {
      yarnClient = new YarnClientImpl();
      yarnClient.init(conf);
      yarnClient.start();
      ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
      yarnClient.killApplication(applicationId);
    } catch (YarnException | IOException e) {
      LOG.log(Level.SEVERE,"Could not close yarn client for killing yarn job");
    } finally {
      if (yarnClient != null) {
        try {
          yarnClient.close();
        } catch (IOException ex) {
          LOG.log(Level.WARNING,
              "Could not close yarn client for killing yarn job");
        }
      }
    }
  }
}
