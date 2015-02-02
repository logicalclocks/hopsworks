package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
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
import se.kth.bbc.jobs.JobControllerEvent;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 * Written for Spark 1.2. If the internals of running Spark on Yarn change,
 * this class may break since it sets environment variables and writes
 * LocalResources that have specific names.
 * <p>
 * @author stig
 */
@ManagedBean
@ViewScoped
public final class SparkController extends JobController {

  private static final Logger logger = Logger.getLogger(
          SparkController.class.getName());

  //Variables for new job
  private String jobName, mainClass, args, appJarName;

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
      super.setBasePath(path);
      super.setJobHistoryFacade(history);
      super.setFileOperations(fops);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize Spark staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Spark controller. Running spark jobs will not work.");
    }
  }

  @Override
  public void afterUploadMainFile(FileUploadEvent event) {
    appJarName = event.getFile().getFileName();
  }
  
  @Override
  public void afterUploadExtraFile(FileUploadEvent event){
    //TODO: allow for file input in Spark
  }

  @Override
  public String getPushChannel() {
    return "/" + sessionState.getActiveStudyname() + "/" + JobType.SPARK;
  }

  public void startJob() {
    SparkYarnRunnerBuilder runnerbuilder = new SparkYarnRunnerBuilder(getMainFilePath(), mainClass);
    runnerbuilder.setJobName(jobName);
    runnerbuilder.setJobArgs(args);
    runnerbuilder.setExtraFiles(getExtraFiles());
    //And that should be it!

    YarnRunner r;
    try {
      r = runnerbuilder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs. Aborting execution.",
              e);
      MessagesController.addErrorMessage("Failed to start Yarn client.");
      return;
    }

    SparkJob job = new SparkJob(history, r, fops);

    setJobId(job.requestJobId(jobName, sessionState.getLoggedInUsername(),
            sessionState.getActiveStudyname(), JobType.SPARK));
    if (isJobSelected()) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + getJobId()
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
    
    @Override
  protected String getUserMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload application jar " + extraInfo + ".";
      case MAIN_UPLOAD_SUCCESS:
        return "Workflow file "+extraInfo+" successfully uploaded.";
      case EXTRA_FILE_FAILURE:
        return "Failed to upload input file " + extraInfo + ".";
      case EXTRA_FILE_SUCCESS:
        return "Input file "+extraInfo+" successfully uploaded.";
      default:
        return super.getUserMessage(event, extraInfo);
    }
  }

  @Override
  protected String getLogMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload application jar " + extraInfo + ".";
      case EXTRA_FILE_FAILURE:
        return "Failed to upload input file " + extraInfo + ".";
      default:
        return super.getLogMessage(event, extraInfo);
    }
  }
}
