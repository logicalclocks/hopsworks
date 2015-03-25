package se.kth.bbc.jobs.yarn;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.apache.hadoop.yarn.exceptions.YarnException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.FileSelectionController;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.JobControllerEvent;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class YarnController extends JobController {

  private static final Logger logger = Logger.getLogger(YarnController.class.
          getName());

  //Variables for new job
  private String jobName, mainClass, args, appMasterName;

  @ManagedProperty(value = "#{clientSessionState}")
  private transient ClientSessionState sessionState;

  @ManagedProperty(value = "#{fileSelectionController}")
  private FileSelectionController fileSelectionController;

  @EJB
  private StagingManager stagingManager;

  @EJB
  private JobHistoryFacade history;

  @EJB
  private FileOperations fops;

  @EJB
  private ActivityFacade activityFacade;

  @EJB
  private AsynchronousJobExecutor submitter;

  @PostConstruct
  public void init() {
    try {
      String path = stagingManager.getStagingPath() + File.separator
              + sessionState.getLoggedInUsername() + File.separator
              + sessionState.getActiveStudyname();
      super.setBasePath(path);
      super.setJobHistoryFacade(history);
      super.setFileOperations(fops);
      super.setFileSelector(fileSelectionController);
      super.setActivityFacade(activityFacade);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize Yarn staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Yarn controller. Running Yarn jobs will not work.");
    }
  }

  @Override
  protected void registerMainFile(String filename,
          Map<String, String> attributes) {
    appMasterName = filename;
  }

  @Override
  protected void registerExtraFile(String filename,
          Map<String, String> attributes) {
    //TODO: allow for input file upload in YARN
  }

  public void startJob() {
    //TODO: add support for input files
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled YARN Job";
    }
    //Construct YarnRunner
    YarnRunner.Builder builder = new YarnRunner.Builder(getMainFilePath(),
            "appMaster.jar");
    builder.amArgs(args).amMainClass(mainClass);
    YarnRunner runner;
    try {
      runner = builder.build();
    } catch (IllegalStateException | IOException e) {
      logger.log(Level.SEVERE, "Could not initialize YarnRunner.", e);
      MessagesController.addErrorMessage("Failed to initialize Yarn client.");
      return;
    }

    //Set up job
    YarnJob job = new YarnJob(history, runner, fops);
    setJobId(job.requestJobId(jobName, sessionState.getLoggedInUsername(),
            sessionState.getActiveStudyname(), JobType.YARN));
    if(isJobSelected()){
      //Set log paths
      String stdOutFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.YARN_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.YARN_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      //Run job
      submitter.startExecution(job);
      MessagesController.addInfoMessage("Job submitted!");
    } else {
      //No job Id has been selected: failed to allocate one. Abort execution.
      logger.
              log(Level.SEVERE, "Failed to persist JobHistory. Aborting execution.");
      MessagesController.addErrorMessage(
              "Failed to write job history. Aborting execution.");
      return;
    }
    writeJobStartedActivity(sessionState.getActiveStudyname(), sessionState.
            getLoggedInUsername());
  }

  @Override
  protected String getUserMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload AM jar " + extraInfo + ".";
      case MAIN_UPLOAD_SUCCESS:
        return "AM jar " + extraInfo + " successfully uploaded.";
      default:
        return super.getUserMessage(event, extraInfo);
    }
  }

  @Override
  protected String getLogMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload AM jar " + extraInfo + ".";
      default:
        return super.getLogMessage(event, extraInfo);
    }
  }

  /*
   * -------------------------------------------------------
   *
   * GETTERS AND SETTERS
   *
   * ---------------------------------------------------------
   */
  public void setFileSelectionController(FileSelectionController fs) {
    this.fileSelectionController = fs;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getAppMasterName() {
    return appMasterName;
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

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

}
