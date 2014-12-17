package se.kth.bbc.cuneiform;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.primefaces.event.FileUploadEvent;
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.yarn.JobController;
import se.kth.bbc.yarn.YarnRunner;

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

  @ManagedProperty(value = "#{studyManagedBean}")
  private StudyMB study;

  private final JobController jc = new JobController();

  public String getWorkflowName() {
    return workflowname;
  }

  public void setWorkflowName(String name) {
    this.workflowname = name;
  }

  @PostConstruct
  public void init() {
    try {
      jc.setBasePath(study.getStudyName(), study.getUsername());
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
    //TODO: display spinner
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

  public void setFreeVars(List<CuneiformParameter> vars) {
    this.freevars = vars;
  }

  public void startWorkflow() {
    YarnRunner.Builder b = new YarnRunner.Builder(Constants.HIWAY_JAR_PATH,
            "Hiway.jar");
    b.appMasterMainClass(
            "de.huberlin.wbi.hiway.app.am.CuneiformApplicationMaster");
    b.stdErrPath("/tmp/stderr.log");
    b.stdOutPath("/tmp/stdout.log");
    
    b.localResourcesBasePath("/user/"+Constants.YARN_USER+"/hiway/"+YarnRunner.APPID_PLACEHOLDER);

    //construct AM arguments
    StringBuilder args = new StringBuilder("--workflow ");
    args.append(getFileName(jc.getFilePath(KEY_WORKFLOW_FILE)));
    args.append(" --appid ");
    args.append(YarnRunner.APPID_PLACEHOLDER);

    b.appMasterArgs(args.toString());

    //Pass on workflow file
    String wfPath = jc.getFilePath(KEY_WORKFLOW_FILE);
    b.addLocalResource(getFileName(wfPath), wfPath, getFileName(wfPath));

    YarnRunner r = b.build();

    //TODO: move to separate thread.
    try {
      r.startAppMaster();
      MessagesController.addInfoMessage("App master started!");
    } catch (YarnException | IOException ex) {
      Logger.getLogger(CuneiformController.class.getName()).
              log(Level.SEVERE, "Failed to start app master", ex);
      MessagesController.
              addErrorMessage("Failed to start app master. See logs.");
    }
  }

  //TODO: move this method to a Utils class (similar method is used elsewhere)
  private static String getFileName(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(startName);
  }

}
