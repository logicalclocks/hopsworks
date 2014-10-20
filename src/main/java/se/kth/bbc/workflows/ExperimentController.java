package se.kth.bbc.workflows;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import se.kth.bbc.study.StudyMB;

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
    private Map<String, Workflow> availableFlows;

    @EJB(beanName = "workflowFacade")
    private WorkflowFacade workflows;

    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB study;

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
        if (availableFlows == null) {
            availableFlows = Maps.uniqueIndex(workflows.findAllForStudy(study.getStudyName()), new Function<Workflow, String>() {
                @Override
                public String apply(Workflow from) {
                    return from.getTitle();
                }
            });
        }
        return availableFlows;
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
        return flowfile == null ? "" : flowfile.getTitle();
    }
}
