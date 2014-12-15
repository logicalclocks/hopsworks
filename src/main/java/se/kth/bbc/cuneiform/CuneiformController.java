package se.kth.bbc.cuneiform;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
/*
 * import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
 * import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
 * import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import org.apache.hadoop.fs.Path;
import org.primefaces.component.selectbooleancheckbox.SelectBooleanCheckbox;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.study.fb.Inode;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.fileoperations.FileSystemOperations;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.yarn.JobController;

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
      for(String s: freenames){
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
    List<String> lines = Files.readAllLines(Paths.get(wfPath), Charset.defaultCharset());
    StringBuilder workflowBuilder = new StringBuilder();
    for(String s:lines){ //TODO: check: does this guarantee line order, is this needed?
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
  
  public List<CuneiformParameter> getFreeVars(){
    return freevars;
  }
  
  public void setFreeVars(List<CuneiformParameter> vars){
    this.freevars = vars;
  }

}
