/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.wf.WorkflowConverter;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@ViewScoped
public class JobTableController implements Serializable{
    
    private List<Job> jobs;
    private Job selectedJob;
    private Job[] selectedJobs;
    private List<Job> selectedJobsList;
    private SelectItem[] jobNamesOptions;

    public JobTableController() {
        jobs= new ArrayList<Job>(JobConverter.jobs.values());
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }
    
      public SelectItem[] getJobNamesAsOptions(){
        jobNamesOptions = 
                createFilterOptions(JobConverter.jobs.keySet().toArray(new String[0]));
        return jobNamesOptions;
    }
    
    private SelectItem[] createFilterOptions(String[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];

        options[0] = new SelectItem("", "Select");
        for(int i = 0; i < data.length; i++) {
            options[i + 1] = new SelectItem(data[i], data[i]);
        }

        return options;
    }

    public Job getSelectedJob() {
        return selectedJob;
    }

    public void setSelectedJob(Job selectedJob) {
        this.selectedJob = selectedJob;
    }
    
    
    
}
