package se.kth.bbc.workflows;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
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

/**
 * Controller for the Experiments tab in StudyPage.
 *
 * @author stig
 */
@ManagedBean
@SessionScoped
public class ExperimentController implements Serializable {

    private String expName;
    private boolean publicFile = false;
    private UploadedFile newFile;

    private Inode workflow;

    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB study;

    @EJB
    private FileOperations fops;

    public ExperimentController() {
    }

    public String getExpName() {
        return expName;
    }

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public Inode getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Inode flowfile) {
        this.workflow = flowfile;
    }

    public StudyMB getStudy() {
        return study;
    }

    public void setStudy(StudyMB study) {
        this.study = study;
    }

    public boolean isWorkflowSet() {
        return this.workflow != null;
    }

    private Map<String, File> getAvailableWorkflows() {
        /*return Maps.uniqueIndex(workflows.findAllForStudyOrPublic(study.getStudyName()), new Function<Workflow, String>() {
         @Override
         public String apply(Workflow from) {
         return from.getWorkflowPK().getTitle();
         }
         });*/
        return null;
    }

    public Collection<String> getWorkflowNames() {
        /*
         Map<String, Workflow> flows = getAvailableWorkflows();
         return flows.keySet();*/
        return null;
    }

    public void setSelectedWorkflow(String title) {
        /*
         setFlowfile(getAvailableWorkflows().get(title));*/

    }

    public String getSelectedWorkflow() {
        /*return flowfile == null ? "" : flowfile.getWorkflowPK().getTitle();*/
        return null;
    }

    public void setPublicFile(boolean pubFile) {
        this.publicFile = pubFile;
    }

    public boolean getPublicFile() {
        return this.publicFile;
    }

    public UploadedFile getUploadFile() {
        return newFile;
    }

    public void setUploadFile(UploadedFile file) {
        this.newFile = file;
    }

    public void handleFileUpload(FileUploadEvent event) {
        //TODO: check if file not already in DB and FS
        newFile = event.getFile();

        //TODO: make more elegant (i.e. don't hard code...)
        String basePath = File.separator + FileSystemOperations.DIR_ROOT + File.separator + study.getStudyName() + File.separator + FileSystemOperations.DIR_CUNEIFORM;
        String destination = basePath + File.separator + newFile.getFileName();

        try {
            //Copy to file system
            InputStream is = newFile.getInputstream();
            fops.writeToHDFS(is, newFile.getSize(), destination);

            //Set current flowfile + notify
            //TODO!!!!! (line below)
            this.workflow = null;
            MessagesController.addInfoMessage("Success", "Cuneiform file was successfully uploaded to HDFS.");
        } catch (IOException e) {
            newFile = null;
            MessagesController.addErrorMessage("fileFailure", "Error", "Upload to HDFS failed.");
        }
    }

    //Needed because PF only calls "process" setters after upload method.
    public void checkboxChanged(AjaxBehaviorEvent event) {
        publicFile = ((SelectBooleanCheckbox) event.getSource()).isSelected();
    }

    /**
     * For the current workflow file, get all the free parameters.
     *
     * @return
     */
    public List<String> getFreeParameters() {
        try {
            //Get the variables
            String txt = getWorkflowText();
            TopLevelContext tlc = StaticNodeVisitor.createTlc(txt);
            return StaticNodeVisitor.getFreeVarNameList(tlc);
        } catch (HasFailedException | IOException e) {
            MessagesController.addErrorMessage("Error!","Failed to load the free variables of the given workflow file.");
        }
        return null;
    }

    //Read the text of the set workflow file
    private String getWorkflowText() throws IOException {
        //Read the cf-file
        StringBuilder workflowBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(fops.getInputStream(workflow)));
        String line = br.readLine();
        while (line != null) {
            workflowBuilder.append(line);
            line = br.readLine();
        }
        return workflowBuilder.toString();
    }

}
