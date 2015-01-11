package se.kth.bbc.jobs.cuneiform;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.RunningJobTracker;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.yarn.AsynchronousYarnApplication;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.StagingManager;

/**
 * Controller for the Cuneiform tab in StudyPage.
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public class CuneiformController implements Serializable {

  private static final String KEY_WORKFLOW_FILE = "WORKFLOW";
  private static final Logger logger = Logger.getLogger(
          CuneiformController.class.getName());

  private String workflowname;
  private boolean workflowUploaded = false;
  private List<CuneiformParameter> freevars;
  private List<CuneiformParameter> targetVars;
  private Long jobhistoryid;
  private boolean started = false;
  private boolean finished = false;
  private String jobName;

  private String stdoutPath;
  private String stderrPath;

  @ManagedProperty(value = "#{studyManagedBean}")
  private StudyMB study;

  @EJB
  private AsynchronousYarnApplication submitter;

  @EJB
  private JobHistoryFacade history;

  @EJB
  private RunningJobTracker runningJobs;

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

  @PostConstruct
  public void init() {
    try {
      String path = stagingManager.getStagingPath() + File.separator + study.getUsername() + File.separator + study.getStudyName();
      jc.setBasePath(path);
    } catch (IOException c) {
      logger.log(Level.SEVERE, "Failed to create directory structure.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize Yarn controller. Running Yarn jobs will not work.");
    }
  }

  public void workflowUpload(FileUploadEvent event) {
    try {
      jc.handleFileUpload(KEY_WORKFLOW_FILE, event);
      workflowUploaded = true;
    } catch (IllegalStateException e) {
      MessagesController.addErrorMessage("Failed to upload file.");
      logger.log(Level.SEVERE, "Illegal state in jobController.");
      init();
      return;
    }
    workflowname = event.getFile().getFileName();
    inspectWorkflow();
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
      
      List<String> targetnames = StaticNodeVisitor.getTargetVarNameList(tlc);
      this.targetVars = new ArrayList<>(targetnames.size());
      for(String s: targetnames) {
        this.targetVars.add(new CuneiformParameter(s,null));
      }
    } catch (HasFailedException | IOException e) {
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
    for (String s : lines) { //TODO: check: does this guarantee line order, is this needed?
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
  
  public List<CuneiformParameter> getTargetVars() {
    return targetVars;
  }

  public void setFreeVars(List<CuneiformParameter> vars) {
    this.freevars = vars;
  }
  
  public void setTargetVars(List<CuneiformParameter> vars){
    this.targetVars = vars;
  }

  public void startWorkflow() {

    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled job";
    }

    YarnRunner.Builder b = new YarnRunner.Builder(Constants.HIWAY_JAR_PATH,
            "Hiway.jar");
    b.appMasterMainClass(
            "de.huberlin.wbi.hiway.app.am.CuneiformApplicationMaster");
    b.appName("Cuneiform " + jobName);

    b.localResourcesBasePath("/user/" + Constants.YARN_USER + "/hiway/"
            + YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
    args.append(getFileName(jc.getFilePath(KEY_WORKFLOW_FILE)));
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);

    b.appMasterArgs(args.toString());

    //Pass on workflow file
    String wfPath = jc.getFilePath(KEY_WORKFLOW_FILE);
    b.addLocalResource(getFileName(wfPath), wfPath, getFileName(wfPath));

    try {
      //Create temp folder for stdout and -err
      Path p = Files.createTempDirectory("BBCTMP");
      b.stdErrPath(Paths.get(p.toString(), "stderr.log").toString());
      b.stdOutPath(Paths.get(p.toString(), "stdout.log").toString());
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory. Stdout and stderr will be unavailable.",
              ex);
      //TODO: make this clear in DB
    }
    //Get the YarnRunner instance
    YarnRunner r = b.build();

    //TODO: include input and execution files
    jobhistoryid = history.create(jobName, study.getUsername(), study.
            getStudyName(), "CUNEIFORM", args.toString(), null,
            "/tmp/stderr.log", "/tmp/stdout.log", null, null);
    if (jobhistoryid != null) {
      submitter.setStdOutFinalDestination(study.getHdfsRootPath()
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jobhistoryid
              + File.separator + "stdout.log");
      submitter.setStdErrFinalDestination(study.getHdfsRootPath()
              + Constants.CUNEIFORM_DEFAULT_OUTPUT_PATH + jobhistoryid
              + File.separator + "stderr.log");
      submitter.registerJob(jobhistoryid, r);
      submitter.handleExecution(jobhistoryid, r);
      MessagesController.addInfoMessage("App master started!");
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      MessagesController.addErrorMessage(
              "Failed to write job history. Aborting execution.");
    }
    started = true;
  }

  //TODO: move this method to a Utils class (similar method is used elsewhere)
  private static String getFileName(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(startName);
  }

  /**
   * Check the progress of the running job. If it is finished, loads the
   * stdout and stderr logs.
   */
  public void checkProgress() {
    if (started) {
      boolean done = !runningJobs.isJobRunning(jobhistoryid);
      if (done) {
        stdoutPath = history.findById(jobhistoryid).getStdoutPath();
        stderrPath = history.findById(jobhistoryid).getStderrPath();
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
  public StreamedContent downloadStdout() {

    StreamedContent sc = null;
    try {
      InputStream is = fops.getInputStream(stdoutPath);
      String extension = "log";
      String filename = "stdout.log";

      sc = new DefaultStreamedContent(is, extension, filename);
      logger.log(Level.INFO, "File was downloaded from HDFS path: {0}",
              stdoutPath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download stdout", ex);
      MessagesController.addErrorMessage(MessagesController.ERROR,
              "Download failed.");
    }
    return sc;
  }

  public StreamedContent downloadStderr() {
    StreamedContent sc = null;
    try {
      InputStream is = fops.getInputStream(stderrPath);
      String extension = "log";
      String filename = "stderr.log";

      sc = new DefaultStreamedContent(is, extension, filename);
      logger.log(Level.INFO, "File was downloaded from HDFS path: {0}",
              stderrPath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download stderr", ex);
      MessagesController.addErrorMessage("Download failed.");
    }
    return sc;
  }

}
