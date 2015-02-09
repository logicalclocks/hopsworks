package se.kth.bbc.jobs.cuneiform;

import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.JobControllerEvent;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 * Controller for the Cuneiform tab in StudyPage.
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public final class CuneiformController extends JobController {

  private static final String KEY_PREFIX_TARGET = "TARGET_";
  private static final Logger logger = Logger.getLogger(
          CuneiformController.class.getName());

  //Variables for new job
  private String workflowname;
  private boolean workflowUploaded = false;
  private List<CuneiformParameter> freevars;
  private List<String> targetVars;
  private List<String> queryVars; //The target variables that should be queried
  private String jobName;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @EJB
  private AsynchronousJobExecutor submitter;

  @EJB
  private JobHistoryFacade history;

  @EJB
  private JobOutputFileFacade jobOutputFacade;

  @EJB
  private FileOperations fops;

  @EJB
  private StagingManager stagingManager;

  public String getWorkflowName() {
    return workflowname;
  }

  public void setWorkflowName(String name) {
    this.workflowname = name;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String name) {
    this.jobName = name;
  }

  public List<String> getQueryVars() {
    return queryVars;
  }

  public void setQueryVars(List<String> queryVars) {
    this.queryVars = queryVars;
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
              "Failed to initialize Cuneiform staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Cuneiform controller. Running Cuneiform jobs will not work.");
    }
  }

  @Override
  protected void afterUploadMainFile(FileUploadEvent event) {
    workflowUploaded = true;
    workflowname = event.getFile().getFileName();
    inspectWorkflow();
  }

  @Override
  protected void afterUploadExtraFile(FileUploadEvent event) {
    String inputVarName = (String) event.getComponent().getAttributes().get(
            "name");
    putVariable(KEY_PREFIX_TARGET + inputVarName, event.getFile().getFileName());
    bindFreeVar(inputVarName, event.getFile().getFileName());
  }

  public boolean isFileUploadedForVar(String name) {
    return variablesContainKey(KEY_PREFIX_TARGET + name);
  }

  private void bindFreeVar(String name, String value) {
    for (CuneiformParameter cp : freevars) {
      if (cp.getName().equals(name)) {
        cp.setValue(value);
        break;
      }
    }
  }

  public boolean isWorkflowUploaded() {
    return workflowUploaded;
  }

  private void inspectWorkflow() {
    try {
      //Get the variables
      String txt = getWorkflowText();
      TopLevelContext tlc = StaticNodeVisitor.createTlc(txt);
      List<String> freenames = StaticNodeVisitor.getFreeVarNameList(tlc);
      this.freevars = new ArrayList<>(freenames.size());
      for (String s : freenames) {
        this.freevars.add(new CuneiformParameter(s, null));
      }

      targetVars = StaticNodeVisitor.getTargetVarNameList(tlc);
      queryVars = new ArrayList<>();
    } catch (Exception e) {//HasFailedException, IOException, but sometimes other exceptions are thrown?
      MessagesController.addErrorMessage(
              "Failed to load the free variables of the given workflow file.");
    }
  }

  //Read the text of the set workflow file
  private String getWorkflowText() throws IOException {
    //Read the cf-file
    String wfPath = getMainFilePath();
    File f = new File(wfPath);
    List<String> lines = Files.readAllLines(Paths.get(wfPath), Charset.
            defaultCharset());
    StringBuilder workflowBuilder = new StringBuilder();
    for (String s : lines) {
      workflowBuilder.append(s);
    }
    return workflowBuilder.toString();

  }

  public ClientSessionState getSessionState() {
    return sessionState;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public List<CuneiformParameter> getFreeVars() {
    return freevars;
  }

  public List<String> getTargetVars() {
    return targetVars;
  }

  public void setFreeVars(List<CuneiformParameter> vars) {
    this.freevars = vars;
  }

  public void setTargetVars(List<String> vars) {
    this.targetVars = vars;
  }

  public void startWorkflow() {
    if(!workflowUploaded){
      MessagesController.addInfoMessage("Upload a workflow first.");
      return;
    }
//TODO: fix state if starting fails
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled Cuneiform job";
    }
    try {
      prepWorkflowFile();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "An error occured while binding parameters in workflow file "
              + workflowname + ".", e);
      MessagesController.addErrorMessage(
              "An error occured while binding parameters in the workflow file. Aborting execution.");
      return;
    }

    String resultName = "results";

    YarnRunner.Builder b = new YarnRunner.Builder(Constants.HIWAY_JAR_PATH,
            "Hiway.jar");
    b.amMainClass(
            "de.huberlin.wbi.hiway.app.am.CuneiformApplicationMaster");
    b.appName("Cuneiform " + jobName);
    b.addAmJarToLocalResources(false); // Weird way of hiway working

    String machineUser = Utils.getYarnUser();

    b.localResourcesBasePath("/user/" + machineUser + "/hiway/"
            + YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
    args.append(Utils.getFileName(getMainFilePath()));
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);
    args.append(" --summary ");
    args.append(resultName);

    b.amArgs(args.toString());

    //Pass on workflow file
    String wfPath = getMainFilePath();
    b.addFilePathToBeCopied(wfPath);

    b.stdOutPath("AppMaster.stdout");
    b.stdErrPath("AppMaster.stderr");
    b.logPathsRelativeToResourcesPath(true);

    YarnRunner r;

    try {
      //Get the YarnRunner instance
      r = b.build();
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs. Aborting execution.",
              ex);
      MessagesController.addErrorMessage("Failed to start Yarn client.");
      return;
    }

    CuneiformJob job = new CuneiformJob(history, fops, r);

    //TODO: include input and execution files
    setJobId(job.requestJobId(jobName, sessionState.getLoggedInUsername(),
            sessionState.getActiveStudyname(), JobType.CUNEIFORM));
    if (isJobSelected()) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      job.setSummaryPath(resultName);
      submitter.startExecution(job);
      MessagesController.addInfoMessage("App master started!");
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      MessagesController.addErrorMessage(
              "Failed to write job history. Aborting execution.");
    }
  }

  public boolean hasOutputFiles() {
    return jobOutputFacade.findOutputFilesForJobid(getJobId()).size() > 0;
  }

  public List<String> getOutputFileNames() {
    List<JobOutputFile> files = jobOutputFacade.findOutputFilesForJobid(
            getJobId());
    List<String> names = new ArrayList<>(files.size());
    for (JobOutputFile file : files) {
      names.add(file.getJobOutputFilePK().getName());
    }
    return names;
  }

  public StreamedContent downloadOutput(String name) {
    //find file from facade, get input stream from path
    JobOutputFile file = jobOutputFacade.findByNameAndJobId(name, getJobId());
    if (file == null) {
      //should never happen
      MessagesController.addErrorMessage(
              "Something went wrong while downloading " + name + ".");
      logger.log(Level.SEVERE,
              "Trying to download an output file that does not exist. JobId:{0}, filename: {1}",
              new Object[]{getJobId(),
                name});
      return null;
    }
    String path = file.getPath();
    try {
      return downloadFile(path, name);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download output file " + name
              + ". Jobid: " + getJobId() + ", path: " + path, ex);
      MessagesController.addErrorMessage("Download failed.");
    }
    return null;
  }

  private void prepWorkflowFile() throws IOException {
    StringBuilder extraLines = new StringBuilder(); //Contains the extra workflow lines
    String foldername = sessionState.getActiveStudyname() + File.separator
            + sessionState.getLoggedInUsername() + File.separator + workflowname
            + File.separator
            + "input"; //folder to which files will be uploaded
    String absoluteHDFSfoldername = "/user/" + System.getProperty("user.name")
            + "/" + foldername;
    //find out which free variables were bound (the ones that have a non-null value)
    for (CuneiformParameter cp : freevars) {
      if (cp.getValue() != null) {
        //copy the input file to where cuneiform expects it
        fops.copyFromLocalNoInode(getFilePath(cp.getValue()),
                absoluteHDFSfoldername + File.separator + cp.getValue());
        //add a line to the workflow file
        extraLines.append(cp.getName()).append(" = '").append(foldername).
                append(File.separator).append(cp.getValue()).append("';\n");
      }
    }
    // for all selected target vars: add "<varname>;" to file
    if (queryVars != null) {
      for (String targetVarName : queryVars) {
        extraLines.append(targetVarName).append(";\n");
      }
    }
    //actually write to workflow file
    String wfPath = getMainFilePath();
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
            wfPath, true)))) {
      out.print(extraLines);
    }
    logger.log(Level.INFO, extraLines.toString());
  }

  public void selectJob(JobHistory job) {
    //TODO
    // overwrite history content and set jobhistoryid
  }

  @Override
  protected String getUserMessage(JobControllerEvent event, String extraInfo) {
    switch (event) {
      case MAIN_UPLOAD_FAILURE:
        return "Failed to upload workflow file " + extraInfo + ".";
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
        return "Failed to upload workflow file " + extraInfo + ".";
      case EXTRA_FILE_FAILURE:
        return "Failed to upload input file " + extraInfo + ".";
      default:
        return super.getLogMessage(event, extraInfo);
    }
  }

}
