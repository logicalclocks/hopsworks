package se.kth.bbc.workflows;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
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
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.upload.FileSystemOperations;

/**
 * Controller for the Experiments tab in StudyPage.
 *
 * @author stig
 */
@ManagedBean
@SessionScoped
public class ExperimentController implements Serializable {

    private String expName;
    private Workflow flowfile;
    private boolean publicFile = false;
    private UploadedFile newFile;

    @EJB(beanName = "workflowFacade")
    private WorkflowFacade workflows;

    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB study;

    @EJB
    private FileSystemOperations fileSystem;

    public ExperimentController() {
    }

    public String getExpName() {
        return expName;
    }

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public Workflow getFlowfile() {
        return flowfile;
    }

    public void setFlowfile(Workflow flowfile) {
        this.flowfile = flowfile;
    }

    public StudyMB getStudy() {
        return study;
    }

    public void setStudy(StudyMB study) {
        this.study = study;
    }

    public void setWorkflows(WorkflowFacade workflows) {
        this.workflows = workflows;
    }

    public boolean isFlowfileSet() {
        return this.flowfile != null;
    }

    private Map<String, Workflow> getAvailableWorkflows() {
        return Maps.uniqueIndex(workflows.findAllForStudyOrPublic(study.getStudyName()), new Function<Workflow, String>() {
            @Override
            public String apply(Workflow from) {
                return from.getWorkflowPK().getTitle();
            }
        });
    }

    public void doSomething() {
        System.out.println("DEBUG: called nothing");
    }

    public Collection<String> getWorkflowNames() {
        Map<String, Workflow> flows = getAvailableWorkflows();
        return flows.keySet();
    }

    public void setSelectedWorkflow(String title) {
        setFlowfile(getAvailableWorkflows().get(title));
    }

    public String getSelectedWorkflow() {
        return flowfile == null ? "" : flowfile.getWorkflowPK().getTitle();
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
        String rootDir = "Workflows";
        String basePath = File.separator + rootDir + File.separator + study.getStudyName();
        Path destination = new Path(basePath + File.separator + newFile.getFileName());

        try {
            //Copy to file system
            InputStream is = newFile.getInputstream();
            fileSystem.copyToHDFS(destination, is);

            //If no problems: write to database
            Workflow wf = new Workflow(newFile.getFileName(), study.getStudyName());
            wf.setPublicFile(publicFile);
            wf.setCreator(study.getUsername());
            workflows.create(wf);

            //Set current flowfile + notify
            this.flowfile = wf;
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Cuneiform file was successfully uploaded to HDFS.");
            FacesContext.getCurrentInstance().addMessage("fileFailure", message);
        } catch (IOException | URISyntaxException ex) {
            newFile = null;
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Upload to HDFS failed.");
            FacesContext.getCurrentInstance().addMessage("fileFailure", message);
        }
    }
    
    //Needed because of bug in PF: only calls "process" setters after upload method.
    public void checkboxChanged(AjaxBehaviorEvent event){
        publicFile = ((SelectBooleanCheckbox)event.getSource()).isSelected();
    }





}
