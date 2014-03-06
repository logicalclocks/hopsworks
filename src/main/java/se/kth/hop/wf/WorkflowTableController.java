/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import se.kth.hop.wf.job.JobDispatcher;
import se.kth.hop.wf.job.JobHistoryFacade;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class WorkflowTableController implements Serializable {
    //@ManagedProperty(value = "#{dispatcher}")

    @EJB
    private JobHistoryFacade jobHistory;
    @EJB
    private WorkflowFacade workflowFacade;
    private JobDispatcher dispatcher;
    private List<Workflow> workflows;
    private Workflow selectedWorkflow;
    private SelectItem[] workflowNamesOptions;

    public WorkflowTableController() {
    }

    @PostConstruct
    public void init() {
        //workflows = new ArrayList<Workflow>(WorkflowConverter.workflows.values());
        workflows = workflowFacade.findAll();
        dispatcher = new JobDispatcher(jobHistory);
        HashSet<Workflow> temp = new HashSet(workflows);
        workflowNamesOptions =
                createFilterOptions(temp.toArray(new Workflow[temp.size()]));
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public SelectItem[] getWfNamesAsOptions() {

        return workflowNamesOptions;
    }

    private SelectItem[] createFilterOptions(Workflow[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];

        options[0] = new SelectItem("", "Select");
        for (int i = 0; i < data.length; i++) {
            options[i + 1] = new SelectItem(data[i].getOwner(), data[i].getOwner());
        }

        return options;
    }

    public Workflow getSelectedWorkflow() {
        return selectedWorkflow;
    }

    public void setSelectedWorkflow(Workflow selectedWorkflow) {
        this.selectedWorkflow = selectedWorkflow;
    }

    @Asynchronous
    public void runSelectedWorkflow() {
        System.out.println(selectedWorkflow.getWorkflowName());
        if (dispatcher == null) {
            System.out.println("null");
        }
        dispatcher.submitWorkflowTask(selectedWorkflow);
    }

    public String removeSelectedWorkflow() {
        workflowFacade.remove(selectedWorkflow);
        return "manageWorkflows";
    }
}
