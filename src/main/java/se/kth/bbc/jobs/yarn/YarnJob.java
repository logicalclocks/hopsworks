package se.kth.bbc.jobs.yarn;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.execution.HopsJob;
import se.kth.bbc.jobs.jobhistory.JobFinalStatus;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServices;
import se.kth.hopsworks.certificates.UserCerts;
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
  protected Map<String, String> projectLocalResources;
  /**
   *
   * @param job
   * @param user
   * @param services
   * @param hadoopDir
   * @param nameNodeIpPort
   * @throws IllegalArgumentException If the JobDescription does not contain a
   * YarnJobConfiguration object.
   */
  public YarnJob(JobDescription job, AsynchronousJobExecutor services,
          Users user, String hadoopDir, String nameNodeIpPort) {
    super(job, services, user, hadoopDir, nameNodeIpPort);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a YarnJobConfiguration object. Received class: "
              + job.getJobConfig().getClass());
    }
    logger.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
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
    } catch (YarnException | IOException e) {
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
  protected boolean setupJob(){
      //Check if this job is using Kakfa, and include certificate
      //in local resources
      Collection<ProjectServices> projectServices =
              jobDescription.getProject().getProjectServicesCollection();
      Iterator<ProjectServices> iter = projectServices.iterator();
      while(iter.hasNext()){
          ProjectServices projectService = iter.next();
          //If the project is of type KAFKA
          if(projectService.getProjectServicesPK().getService() == ProjectServiceEnum.KAFKA){
              //Pull the certificate of the client
              UserCerts userCert = services.getUserCerts().findUserCert(
                  projectService.getProject().getId(),
                  projectService.getProject().getOwner().getUid());
              //Retrieve certificates from the database
              Map<String, byte[]> kafkaCertFiles = new HashMap<>();
              kafkaCertFiles.put(Settings.KAFKA_K_CERTIFICATE, userCert.getUserCert());
              kafkaCertFiles.put(Settings.KAFKA_T_CERTIFICATE, userCert.getUserKey());
              Map<String, File> kafkaCerts = new HashMap<>();
              //Create tmp cert directory if not exists
              File certDir = new File("/srv/glassfish/kafkacerts");
              if (!certDir.exists()) {
                    try{
                        certDir.mkdir();
                    } 
                    catch(SecurityException ex){
                        logger.log(Level.SEVERE, ex.getMessage());//handle it
                    }        
                   
              }
              try{
                kafkaCerts.put(Settings.KAFKA_K_CERTIFICATE, new File(
                        "/srv/glassfish/kafkacerts/" +
                                projectService.getProject().getId()+ "__" +
                                projectService.getProject().getOwner().getUid() +
                                "__kstore.jks"));
                kafkaCerts.put(Settings.KAFKA_T_CERTIFICATE, new File(
                        "/srv/glassfish/kafkacerts/" +
                                projectService.getProject().getId()+ "__" +
                                projectService.getProject().getOwner().getUid() +
                                "__tstore.jks"));
                if(projectLocalResources == null){
                   projectLocalResources = new HashMap<>();
                }
                // if file doesnt exists, then create it
                try {
                  for(Map.Entry<String, File> entry : kafkaCerts.entrySet()){
                    if (!entry.getValue().exists()) {
                        entry.getValue().createNewFile();
                    }
                  
                    Files.write(kafkaCertFiles.get(entry.getKey()), entry.getValue());
                    services.getFsService().getDfsOps().copyToHDFSFromLocal(true, entry.getValue().getAbsolutePath(), "/user/glassfish");
                    projectLocalResources.put(entry.getKey(), "hdfs://"+nameNodeIpPort+"/user/glassfish/"+entry.getValue().getName());
                  }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, 
                            "Error writing Kakfa certificates to local fs", ex);
                }
                
               } finally{
                  //In case the certificates where not removed
                  for(Map.Entry<String, File> entry : kafkaCerts.entrySet()){
                      if(entry.getValue().exists()){
                          entry.getValue().delete();
                      }
                  }
              }
          }
         
      }
      
      return true;
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
      return true;
    }
  }

  /**
   * Copy the AM logs to their final destination.
   */
  protected void copyLogs() {
    try {
      if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs() && !runner.areLogPathsAggregated()) {
          services.getFileOperations(hdfsUser.getUserName()).
                  copyToHDFSFromLocal(true, runner.
                          getStdOutPath(),
                          stdOutFinalDestination);
        } else if (runner.areLogPathsAggregated()) {
          YarnLogUtil.copyAggregatedYarnLogs(services.getFsService(),
                  services.getFileOperations(hdfsUser.getUserName()), runner.
                  getStdOutPath(),
                  stdOutFinalDestination, "stdout");

        } else {
          services.getFileOperations(hdfsUser.getUserName()).renameInHdfs(
                  runner.
                  getStdOutPath(),
                  stdOutFinalDestination);
        }
      }
      if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
        if (!runner.areLogPathsHdfs() && !runner.areLogPathsAggregated()) {
          services.getFileOperations(hdfsUser.getUserName()).
                  copyToHDFSFromLocal(true, runner.
                          getStdErrPath(),
                          stdErrFinalDestination);
        } else if (runner.areLogPathsAggregated()) {
          YarnLogUtil.copyAggregatedYarnLogs(services.getFsService(),
                  services.getFileOperations(hdfsUser.getUserName()), runner.
                  getStdOutPath(),
                  stdErrFinalDestination, "stderr");
        } else {
          services.getFileOperations(hdfsUser.getUserName()).renameInHdfs(
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
  protected void runJob() {
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
    updateState(JobState.AGGREGATING_LOGS);
    copyLogs();
    updateState(getFinalState());
  }

  @Override
  //DOESN'T WORK FOR NOW
  protected void stopJob(String appid) {
    try {
      YarnClient yarnClient = new YarnClientImpl();
      yarnClient.init(conf);
      yarnClient.start();
      ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
      yarnClient.killApplication(applicationId);
    } catch (YarnException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



}
