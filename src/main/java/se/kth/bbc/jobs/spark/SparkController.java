package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 * Written for Spark 1.2
 * @author stig
 */
@ManagedBean
@ViewScoped
public class SparkController implements Serializable {

  private static final Logger logger = Logger.getLogger(
          SparkController.class.getName());
  private static final String KEY_APP_JAR = "APPJAR";

  //Variables for new job
  private String jobName, mainClass, args, appJarName;

  //Used to track job that was last executed (or selected)
  private Long jobhistoryid;
  private final JobController jc = new JobController();

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @EJB
  private AsynchronousJobExecutor submitter;

  @EJB
  private JobHistoryFacade history;

  @EJB
  private FileOperations fops;

  @EJB
  private StagingManager stagingManager;

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public String getAppJarName() {
    return appJarName;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  @PostConstruct
  public void init() {
    try {
      String path = stagingManager.getStagingPath() + File.separator
              + sessionState.getLoggedInUsername() + File.separator
              + sessionState.getActiveStudyname();
      jc.setBasePath(path);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Yarn controller. Running Yarn jobs will not work.");
    }
  }

  public void appJarUpload(FileUploadEvent event) {
    try {
      jc.clearFiles();
      jc.clearVariables();
      jc.handleFileUpload(KEY_APP_JAR, event);
    } catch (IllegalStateException e) {
      MessagesController.addErrorMessage("Failed to upload app jar "
              + event.getFile().getFileName());
      logger.log(Level.SEVERE,
              "Illegal state in jobController while trying to upload app jar.",
              e);
      return;
    }
    appJarName = event.getFile().getFileName();
  }

  public boolean isSelectedJobRunning() {
    if (jobhistoryid == null) {
      return false;
    } else {
      return !jobHasFinishedState();
    }
  }

  public JobHistory getSelectedJob() {
    if (jobhistoryid == null) {
      return null;
    } else {
      return history.findById(jobhistoryid);
    }
  }

  public String getPushChannel() {
    return "/" + sessionState.getActiveStudyname() + "/" + JobType.SPARK;
  }

  private boolean jobHasFinishedState() {
    JobState state = history.getState(jobhistoryid);
    if (state == null) {
      return true;
    }
    return state.isFinalState();
  }

  public void startJob() {
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled Spark job";
    }

    YarnRunner.Builder builder = new YarnRunner.Builder(Constants.SPARK_AM_MAIN);
    Map<String, String> extraFiles = new HashMap<>();

    //Spark staging directory
    String stagingPath = File.separator + "user" + File.separator + Utils.
            getYarnUser() + File.separator + Constants.SPARK_STAGING_DIR
            + File.separator + YarnRunner.APPID_PLACEHOLDER;

    builder.localResourcesBasePath(stagingPath);

    //Add app and spark jar
    //TODO: check if you can remove these?
    builder.addLocalResource(Constants.SPARK_LOCRSC_SPARK_JAR,
            Constants.DEFAULT_SPARK_JAR_PATH);
    builder.addLocalResource(Constants.SPARK_LOCRSC_APP_JAR, jc.getFilePath(
            KEY_APP_JAR));
    
    //Add extra files to local resources, as key: use filename
    for (Map.Entry<String, String> k : extraFiles.entrySet()) {
      builder.addLocalResource(k.getKey(), k.getValue());
    }

    //TODO: add to classpath: user specified jars, extra classes from conf file
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", Utils.getYarnUser());
    builder.addToAppMasterEnvironment("CLASSPATH", "/srv/spark/conf:/srv/spark/lib/spark-assembly-1.2.0-hadoop2.4.0.jar:/srv/spark/lib/datanucleus-core-3.2.10.jar:/srv/spark/lib/datanucleus-api-jdo-3.2.6.jar:/srv/spark/lib/datanucleus-rdbms-3.2.9.jar");

    //Add local resources to spark environment too
    builder.addCommand(new SparkSetEnvironmentCommand());
    
    //TODO: add env vars from sparkconf to path

    //TODO add java options from spark config (or not...)
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(mainClass);
    amargs.append(" --num-executors 1 ");
    amargs.append(" --executor-cores 1 ");
    amargs.append(" --executor-memory 512m");
    if (args != null && !args.isEmpty()) {
      amargs.append(" --arg ");
      amargs.append(args);
    }
    builder.amArgs(amargs.toString());

    builder.appName(jobName);
    //And that should be it!

    YarnRunner r;
    try {
      r = builder.build();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs. Aborting execution.",
              e);
      MessagesController.addErrorMessage("Failed to start Yarn client.");
      return;
    }

    SparkJob job = new SparkJob(history, r, fops);

    jobhistoryid = job.requestJobId(jobName, sessionState.getLoggedInUsername(),
            sessionState.getActiveStudyname(), JobType.SPARK);
    if (jobhistoryid != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jobhistoryid
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jobhistoryid
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(job);
      MessagesController.addInfoMessage("Job submitted!");
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      MessagesController.addErrorMessage(
              "Failed to write job history. Aborting execution.");
    }
  }
}
