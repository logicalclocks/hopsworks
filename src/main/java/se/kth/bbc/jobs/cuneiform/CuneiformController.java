package se.kth.bbc.jobs.cuneiform;

import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
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
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobOutputFile;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 * Controller for the Cuneiform tab in StudyPage.
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class CuneiformController implements Serializable {

  private static final String KEY_WORKFLOW_FILE = "WORKFLOW";
  private static final String KEY_PREFIX_TARGET = "TARGET_";
  private static final Logger logger = Logger.getLogger(
          CuneiformController.class.getName());

  private String workflowname;
  private boolean workflowUploaded = false;
  private List<CuneiformParameter> freevars;
  private List<String> targetVars;
  private List<String> queryVars; //The target variables that should be queried
  private Long jobhistoryid;
  private boolean started = false;
  private boolean finished = false;
  private String jobName;

  private JobState finalState;

  private String stdoutPath;
  private String stderrPath;

  @ManagedProperty(value = "#{studyManagedBean}")
  private StudyMB study;

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

  private final JobController jc = new JobController();

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
      String path = stagingManager.getStagingPath() + File.separator + study.
              getUsername() + File.separator + study.getStudyName();
      jc.setBasePath(path);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Yarn controller. Running Yarn jobs will not work.");
    }
  }

  public void workflowUpload(FileUploadEvent event) {
    try {
      jc.clearFiles();
      jc.clearVariables();
      jc.handleFileUpload(KEY_WORKFLOW_FILE, event);
      workflowUploaded = true;
    } catch (IllegalStateException e) {
      MessagesController.addErrorMessage("Failed to upload workflow file "
              + event.getFile().getFileName());
      logger.log(Level.SEVERE,
              "Illegal state in jobController while trying to upload workflow file.",
              e);
      init(); //just try to fix it...
      return;
    }
    workflowname = event.getFile().getFileName();
    inspectWorkflow();
  }

  public void inputFileUpload(FileUploadEvent event) {
    String inputVarName = (String) event.getComponent().getAttributes().get(
            "name");
    try {
      String filename = event.getFile().getFileName();
      jc.handleFileUpload(filename, event);
      jc.putVariable(KEY_PREFIX_TARGET + inputVarName, filename);
      bindFreeVar(inputVarName, filename);
    } catch (IllegalStateException e) {
      MessagesController.addErrorMessage("Failed to upload input file " + event.
              getFile().getFileName() + " for input variable " + inputVarName);
      logger.log(Level.SEVERE,
              "Illegal state in jobController while trying to upload input file "
              + event.getFile().getFileName() + " for input variable "
              + inputVarName + ", workflow file: " + workflowname + ".",
              e);
    }
  }

  public boolean isFileUploadedForVar(String name) {
    return jc.containsVariableKey(KEY_PREFIX_TARGET + name);
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
    String wfPath = jc.getFilePath(KEY_WORKFLOW_FILE);
    File f = new File(wfPath);
    List<String> lines = Files.readAllLines(Paths.get(wfPath), Charset.
            defaultCharset());
    StringBuilder workflowBuilder = new StringBuilder();
    for (String s : lines) {
      workflowBuilder.append(s);
    }
    return workflowBuilder.toString();

  }

  public StudyMB getStudy() {
    return study;
  }

  public void setStudy(StudyMB study) {
    this.study = study;
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
//TODO: fix state if starting fails
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled job";
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

    String machineUser = System.getProperty("user.name");
    if (machineUser == null) {
      machineUser = Constants.DEFAULT_YARN_USER;
      logger.log(Level.WARNING,
              "Username not found in system properties, using default \"glassfish\"");
    }

    b.localResourcesBasePath("/user/" + machineUser + "/hiway/"
            + YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
    args.append(Utils.getFileName(jc.getFilePath(KEY_WORKFLOW_FILE)));
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);
    args.append(" --summary ");
    args.append(resultName);

    b.amArgs(args.toString());

    //Pass on workflow file
    String wfPath = jc.getFilePath(KEY_WORKFLOW_FILE);
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
    jobhistoryid = job.requestJobId(jobName, study.getUsername(), study.
            getStudyName(), "CUNEIFORM");
    if (jobhistoryid != null) {
      String stdOutFinalDestination = study.getHdfsRootPath()
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jobhistoryid
              + File.separator + "stdout.log";
      String stdErrFinalDestination = study.getHdfsRootPath()
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jobhistoryid
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
    started = true;
  }

  /**
   * Check the progress of the running job. If it is finished, loads the
   * stdout and stderr logs.
   */
  public void checkProgress() {
    if (started) {
      boolean done = jobHasFinishedState();
      if (done) {
        stdoutPath = history.findById(jobhistoryid).getStdoutPath();
        stderrPath = history.findById(jobhistoryid).getStderrPath();
        finalState = history.findById(jobhistoryid).getState();
        //Read stdout
        /*
         * StringBuilder stdOutBuilder = new StringBuilder();
         * try (InputStream in = fops.getInputStream(stdOutPath)) {
         * BufferedReader reader = new BufferedReader(new
         * InputStreamReader(in));
         * String line = null;
         * while ((line = reader.readLine()) != null) {
         * stdOutBuilder.append(line);
         * //stdOutBuilder.append("\n");
         * }
         * } catch (IOException e) {
         * logger.log(Level.SEVERE, "Failed loading stdout", e);
         * stdOutBuilder.append("ERROR LOADING STDOUT");
         * }
         * stdout = stdOutBuilder.toString();
         */
        //Read stdErr
       /*
         * StringBuilder stdErrBuilder = new StringBuilder();
         * try (InputStream in = fops.getInputStream(stdErrPath)) {
         * BufferedReader reader = new BufferedReader(new
         * InputStreamReader(in));
         * String line = null;
         * while ((line = reader.readLine()) != null) {
         * stdErrBuilder.append(line);
         * //stdErrBuilder.append("\n");
         * }
         * } catch (IOException e) {
         * logger.log(Level.SEVERE, "Failed loading stderr", e);
         * stdErrBuilder.append("ERROR LOADING STDERR");
         * }
         * stderr = stdErrBuilder.toString();
         */
        finished = true;
      }
    }
  }

  private boolean jobHasFinishedState() {
    JobState state = history.getState(jobhistoryid);
    if (state == null) {
      //should never happen
      return true;
    }
    return state == JobState.FAILED || state == JobState.FRAMEWORK_FAILURE
            || state == JobState.FINISHED || state == JobState.KILLED;
  }

  public boolean isJobFinished() {
    return finished;
  }

  public boolean isJobStarted() {
    return started;
  }

  /*
   * public String getStdOut(){
   * return stdout;
   * }
   *
   * public String getStdErr(){
   * return stderr;
   * }
   */
  //TODO: move download methods to JobHistoryController
  public StreamedContent downloadStdout() {
    try {
      String extension = "log";
      String filename = "stdout.log";
      return downloadFile(stdoutPath, filename);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download stdout. JobId: "
              + jobhistoryid + ", path: " + stdoutPath, ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Download failed.");
    }
    return null;
  }

  public StreamedContent downloadStderr() {
    String extension = "log";
    String filename = "stderr.log";
    try {
      return downloadFile(stderrPath, filename);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download stderr. JobId: "
              + jobhistoryid + ", path: " + stderrPath, ex);
      MessagesController.addErrorMessage("Download failed.");
    }
    return null;
  }

  private StreamedContent downloadFile(String path,
          String filename) throws IOException {
    InputStream is = fops.getInputStream(path);
    StreamedContent sc = new DefaultStreamedContent(is, Utils.getMimeType(filename),
            filename);
    logger.log(Level.INFO, "File was downloaded from HDFS path: {0}",
            path);
    return sc;
  }

  public JobState getFinalState() {
    if (!finished) {
      return JobState.RUNNING;
    } else {
      return finalState;
    }
  }

  public boolean shouldShowDownload() {
    if (!finished) {
      return false;
    } else if (finalState == JobState.FINISHED) {
      return true;
    }
    return false;
  }

  public boolean hasOutputFiles() {
    return jobOutputFacade.findOutputFilesForJobid(jobhistoryid).size() > 0;
  }

  public List<String> getOutputFileNames() {
    List<JobOutputFile> files = jobOutputFacade.findOutputFilesForJobid(
            jobhistoryid);
    List<String> names = new ArrayList<>(files.size());
    for (JobOutputFile file : files) {
      names.add(file.getJobOutputFilePK().getName());
    }
    return names;
  }

  public StreamedContent downloadOutput(String name) {
    //find file from facade, get input stream from path
    JobOutputFile file = jobOutputFacade.findByNameAndJobId(name, jobhistoryid);
    if (file == null) {
      //should never happen
      MessagesController.addErrorMessage(
              "Something went wrong while downloading " + name + ".");
      logger.log(Level.SEVERE,
              "Trying to download an output file that does not exist. JobId:{0}, filename: {1}",
              new Object[]{jobhistoryid,
                name});
      return null;
    }
    String path = file.getPath();
    String extension = Utils.getExtension(name);
    try {
      return downloadFile(path, name);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download output file " + name
              + ". Jobid: " + jobhistoryid + ", path: " + path, ex);
      MessagesController.addErrorMessage("Download failed.");
    }
    return null;
  }

  private void prepWorkflowFile() throws IOException {
    StringBuilder extraLines = new StringBuilder(); //Contains the extra workflow lines
    String foldername = study.getStudyName() + File.separator + study.
            getUsername() + File.separator + workflowname + File.separator
            + "input"; //folder to which files will be uploaded
    String absoluteHDFSfoldername = "/user/" + System.getProperty("user.name")
            + "/" + foldername;
    //find out which free variables were bound (the ones that have a non-null value)
    for (CuneiformParameter cp : freevars) {
      if (cp.getValue() != null) {
        //copy the input file to where cuneiform expects it
        fops.copyFromLocalNoInode(jc.getFilePath(cp.getValue()),
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
    String wfPath = jc.getFilePath(KEY_WORKFLOW_FILE);
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
            wfPath, true)))) {
      out.print(extraLines);
    }
    logger.log(Level.INFO, extraLines.toString());
  }

}
