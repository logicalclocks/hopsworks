/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.wf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Asynchronous;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.job.JobDispatcher;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class WorkflowTableController implements Serializable{
    //@ManagedProperty(value = "#{dispatcher}")
    private JobDispatcher dispatcher;
    private List<Workflow> workflows;
    private Workflow selectedWorkflow;
    private Workflow[] selectedWorkflows;
    private List<Workflow> selectedWorkflowsList;
    private SelectItem[] workflowNamesOptions;

    public WorkflowTableController() {
        workflows = new ArrayList<Workflow>(WorkflowConverter.workflows.values());
        dispatcher = new JobDispatcher();
    }

    public List<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }
    
    public SelectItem[] getWfNamesAsOptions(){
        workflowNamesOptions = 
                createFilterOptions(WorkflowConverter.workflows.keySet().toArray(new String[0]));
        return workflowNamesOptions;
    }
    
    private SelectItem[] createFilterOptions(String[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];

        options[0] = new SelectItem("", "Select");
        for(int i = 0; i < data.length; i++) {
            options[i + 1] = new SelectItem(data[i], data[i]);
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
    public void runSelectedWorkflow(){
        System.out.println(selectedWorkflow.getWorkflowName());
        if(dispatcher==null){
            System.out.println("null");
        }
        dispatcher.submitWorkflowTask(selectedWorkflow);
    }
}
