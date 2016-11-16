package se.kth.bbc.jobs.yarn;

import java.io.File;
import java.io.IOException;
import com.google.common.io.Files;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobFinalStatus;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServices;
import se.kth.hopsworks.certificates.UserCerts;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author stig
 */
public abstract class YarnJob extends HopsJob {

  private static final Logger logger = Logger.getLogger(YarnJob.class.getName());

  private static final int DEFAULT_MAX_STATE_POLL_RETRIES = 10;
  private static final int DEFAULT_POLL_TIMEOUT_INTERVAL = 1; //in seconds

  protected YarnRunner runner;

  protected YarnMonitor monitor = null;
  private Configuration conf = new Configuration();

  private String stdOutFinalDestination, stdErrFinalDestination;
  private boolean started = false;

  private JobState finalState = null;
  protected List<LocalResourceDTO> projectLocalResources;
  protected Map<String, String> jobSystemProperties;

  protected String kafkaAddress;
  protected String restEndpoint;
  protected final String jobUser;

  /**
   *
   * @param job
   * @param user
   * @param services
   * @param jobUser
   * @param hadoopDir
   * @param nameNodeIpPort
   * @param kafkaAddress
   * @param restEndpoint
   * @throws IllegalArgumentException If the JobDescription does not contain a
   * YarnJobConfiguration object.
   */
  public YarnJob(JobDescription job, AsynchronousJobExecutor services,
          Users user, String jobUser, String hadoopDir, String nameNodeIpPort,
          String kafkaAddress, String restEndpoint) {
    super(job, services, user, hadoopDir, nameNodeIpPort);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a YarnJobConfiguration object. Received class: "
              + job.getJobConfig().getClass());
    }
    logger.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
    this.kafkaAddress = kafkaAddress;
    this.restEndpoint = restEndpoint;
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
   * <p/>
   * @return True if the AM was started, false otherwise.
   * @throws IllegalStateException If the YarnRunner has not been set yet.
   */
  protected final boolean startApplicationMaster() throws IllegalStateException {
    if (runner == null) {
      throw new IllegalStateException(
              "The YarnRunner has not been initialized yet.");
    }
    try {
      updateState(JobState.STARTING_APP_MASTER);
      monitor = runner.startAppMaster();
      started = true;
      updateExecution(null, -1, null, null, monitor.getApplicationId().
              toString(), null, null, null, 0);
      return true;
    } catch (AccessControlException ex) {
      logger.log(Level.SEVERE, "Permission denied:- {0}", ex.getMessage());
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } catch (YarnException | IOException  | URISyntaxException e) {
      logger.log(Level.SEVERE,
              "Failed to start application master for execution "
              + getExecution()
              + ". Aborting execution",
              e);
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    //Check if this job is using Kakfa, and include certificate
    //in local resources
    Collection<ProjectServices> projectServices = jobDescription.getProject().
            getProjectServicesCollection();
    Iterator<ProjectServices> iter = projectServices.iterator();
    while (iter.hasNext()) {
      ProjectServices projectService = iter.next();
      //If the project is of type KAFKA
      if (projectService.getProjectServicesPK().getService()
              == ProjectServiceEnum.KAFKA && (jobDescription.getJobType()
              == JobType.FLINK || jobDescription.getJobType() == JobType.SPARK) &&
              jobDescription.getJobConfig() instanceof YarnJobConfiguration &&
              ((YarnJobConfiguration)jobDescription.getJobConfig()).isKafka()) {
        copyUserKafkaCerts(projectService, true,
                Settings.KAFKA_TMP_CERT_STORE_LOCAL,
                Settings.KAFKA_TMP_CERT_STORE_REMOTE, dfso);
        return true;
      }
    }
    return true;
  }

  /**
   * Utility method that copies Kafka user certificates from the Database, to
   * either hdfs to be passed as LocalResources to the YarnJob or to used
   * by another method.
   *
   * @param projectService
   * @param isYarnJob
   */
  private void copyUserKafkaCerts(ProjectServices projectService,
          boolean isYarnJob, String localTmpDir, String remoteTmpDir,
          DistributedFileSystemOps dfso) {
    //Pull the certificate of the client
    UserCerts userCert = services.getUserCerts().findUserCert(
            projectService.getProject().getName(),
            projectService.getProject().getOwner().getUsername());
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null && userCert.getUserCert().length > 0
            && userCert.getUserKey() != null && userCert.getUserKey().length > 0) {

      Map<String, byte[]> kafkaCertFiles = new HashMap<>();
      kafkaCertFiles.put(Settings.KAFKA_K_CERTIFICATE, userCert.getUserCert());
      kafkaCertFiles.put(Settings.KAFKA_T_CERTIFICATE, userCert.getUserKey());
      Map<String, File> kafkaCerts = new HashMap<>();
      //Create tmp cert directory if not exists. Certificates will later 
      //be deleted from this directory when copied to HDFS. 
      File certDir = new File(Settings.KAFKA_TMP_CERT_STORE_LOCAL);
      if (!certDir.exists()) {
        try {
          certDir.mkdir();
        } catch (SecurityException ex) {
          logger.log(Level.SEVERE, ex.getMessage());//handle it
        }

      }
      //Setting of files as system params
      //Set projectId as java system property
      if (isYarnJob) {
        jobSystemProperties.put(Settings.KAFKA_PROJECTID_ENV_VAR,
                String.valueOf(projectService.getProject().getId()));
      }

      try {        
        String k_certName = projectService.getProject().getName() + "__"
                + projectService.getProject().getOwner().getUsername()
                + "__kstore.jks";
        String t_certName = projectService.getProject().getName() + "__"
                + projectService.getProject().getOwner().getUsername()
                + "__tstore.jks";
        
        kafkaCerts.put(Settings.KAFKA_K_CERTIFICATE, new File(
                localTmpDir + "/" + k_certName));
        kafkaCerts.put(Settings.KAFKA_T_CERTIFICATE, new File(
                localTmpDir + "/" + t_certName));
        // if file doesnt exists, then create it
        try {
          //If it is a Flink job, copy the certificates into the glassfish dir
          if (jobDescription.getJobType() == JobType.FLINK) {
            File f_k_cert = new File("/srv/glassfish/domain1/config/"
                    + k_certName);
            File t_k_cert = new File("/srv/glassfish/domain1/config/"
                    + t_certName);
            if (!f_k_cert.exists()) {
              Files.write(kafkaCertFiles.get(Settings.KAFKA_K_CERTIFICATE),
                      f_k_cert);
              Files.write(kafkaCertFiles.get(Settings.KAFKA_T_CERTIFICATE),
                      t_k_cert);
            }
          }
          for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
            if (!entry.getValue().exists()) {
              entry.getValue().createNewFile();
            }

            //Write the actual file(cert) to localFS
            //Create HDFS kafka certificate directory. This is done
            //So that the certificates can be used as LocalResources
            //by the YarnJob
            
            if (!dfso.exists(remoteTmpDir)) {
              dfso.mkdir(
                      new Path(remoteTmpDir), new FsPermission(FsAction.ALL,
                              FsAction.ALL, FsAction.ALL));
            }
            //Put project certificates in its own dir
            String certUser = projectService.getProject().getName() + "__"
                    + projectService.getProject().getOwner().getUsername();
            String remoteTmpProjDir = remoteTmpDir + File.separator + certUser;
            if (!dfso.exists(remoteTmpProjDir)) {
              dfso.mkdir(
                      new Path(remoteTmpProjDir),
                      new FsPermission(FsAction.ALL,
                              FsAction.ALL, FsAction.NONE));
              dfso.setOwner(new Path(remoteTmpProjDir),
                      certUser,
                      certUser);
            }
            Files.write(kafkaCertFiles.get(entry.getKey()), entry.getValue());
            dfso.copyToHDFSFromLocal(true, entry.getValue().getAbsolutePath(),
                    remoteTmpDir + File.separator + certUser + File.separator
                    + entry.getValue().getName());

            dfso.setPermission(new Path(remoteTmpProjDir + File.separator
                    + entry.getValue().getName()),
                    new FsPermission(FsAction.ALL, FsAction.NONE,
                            FsAction.NONE));
            dfso.setOwner(new Path(remoteTmpProjDir + File.separator + entry.
                    getValue().getName()),
                    certUser,
                    certUser);

            if (isYarnJob) {
              projectLocalResources.add(new LocalResourceDTO(
                      entry.getKey(),
                      "hdfs://" + nameNodeIpPort + remoteTmpDir +
                      File.separator + projectService.getProject().getName() + "__"
                      + projectService.getProject().getOwner().getUsername()+File.separator+entry.getValue().getName(),
                      LocalResourceVisibility.APPLICATION.toString(),
                      LocalResourceType.FILE.toString(), null));

              jobSystemProperties.
                      put(entry.getKey(), entry.getValue().getName());
            }
          }
        } catch (IOException ex) {
          logger.log(Level.SEVERE,
                  "Error writing Kakfa certificates to local fs", ex);
        }

      } finally {
        //In case the certificates where not removed
        for (Map.Entry<String, File> entry : kafkaCerts.entrySet()) {
          if (entry.getValue().exists()) {
            entry.getValue().delete();
          }
        }
      }
    }
  }

  /**
   * Monitor the state of the job.
   * <p/>
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
        logger.log(Level.WARNING,
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
          logger.log(Level.WARNING,
                  "Failed to get application state for execution "
                  + getExecution() + ". Tried " + failures + " time(s).", ex);
        }
      }

      if (failures > DEFAULT_MAX_STATE_POLL_RETRIES) {
        try {
          logger.log(Level.SEVERE,
                  "Killing application, {0}, because unable to poll for status.",
                  getExecution());
          r.cancelJob(r.getApplicationId().toString());
          updateState(JobState.KILLED);
          updateFinalStatus(JobFinalStatus.KILLED);
          updateProgress(0);
          finalState = JobState.KILLED;
        } catch (YarnException | IOException ex) {
          logger.log(Level.SEVERE,
                  "Failed to cancel execution, " + getExecution()
                  + " after failing to poll for status.", ex);
          updateState(JobState.FRAMEWORK_FAILURE);
          finalState = JobState.FRAMEWORK_FAILURE;
        }
        return false;
      }
      finalState = JobState.getJobState(appState);
//      if(finalState == JobState.FINISHED){
//          updateJobHistoryApp(monitor.getApplicationId().toString());
//      }
      return true;
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
        stdOutFinalDestination = stdOutFinalDestination + getExecution().getAppId()
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
        stdErrFinalDestination = stdErrFinalDestination + getExecution().getAppId()
            + File.separator + "stderr.log";
        if (!runner.areLogPathsHdfs() && !runner.areLogPathsAggregated()) {
          udfso.copyToHDFSFromLocal(true, runner.
                  getStdErrPath(),
                  stdErrFinalDestination);
        } else if (runner.areLogPathsAggregated()) {
          String[] desiredLogTypes = {"err",".log"};
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
      logger.log(Level.SEVERE,
              "Exception while trying to write logs for execution "
              + getExecution() + " to HDFS.", e);
    }
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso) {
    // Try to start the AM
    boolean proceed = startApplicationMaster();

    if (!proceed) {
      return;
    }
    proceed = monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    try {
      runner.removeAllNecessary();
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              "Exception while trying to delete job tmp files "
              + getExecution(), ex);
    }
    updateState(JobState.AGGREGATING_LOGS);
    copyLogs(udfso);
    updateState(getFinalState());
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
    } catch (YarnException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if(yarnClient!=null){
        try {
          yarnClient.close();
        } catch (IOException ex) {
          logger.log(Level.WARNING, "Could not close yarn client for killing yarn job");
        }
      }
    }
  }
}
