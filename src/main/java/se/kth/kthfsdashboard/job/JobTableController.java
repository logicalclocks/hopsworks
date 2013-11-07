/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@ViewScoped
public class JobTableController implements Serializable {

//    @EJB
//    private JobHistoryFacade jobHistory;
    private List<Job> jobs;
    private List<String> filter;
    private Job selectedJob;
    private Job[] selectedJobs;
    private List<Job> selectedJobsList;
    private SelectItem[] jobNamesOptions;
    

    public JobTableController() {
    }

    @PostConstruct
    public void init() {
//        jobs=jobHistory.findAll();
        jobs = new ArrayList<Job>();
        filter = new ArrayList<String>();
        for(Job job:jobs){
            filter.add(job.getExecutedBy());
        }
        
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public SelectItem[] getJobNamesAsOptions() {
        jobNamesOptions =
                createFilterOptions(filter.toArray(new String[0]));
        return jobNamesOptions;
    }

    private SelectItem[] createFilterOptions(String[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];

        options[0] = new SelectItem("", "Select");
        for (int i = 0; i < data.length; i++) {
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
