/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class JobTableController implements Serializable {

    @EJB
    private JobHistoryFacade jobHistory;
    private List<Job> jobs;
    private Job selectedJob;
    private Job[] selectedJobs;
    private List<Job> selectedJobsList;
    private SelectItem[] jobNamesOptions;
    private Set<String> filter;

    public JobTableController() {
    }

    @PostConstruct
    public void init() {
        jobs = jobHistory.findAll();

//        jobs = new ArrayList<Job>(converter.jobs.values());
        //jobs = new ArrayList<Job>();
        filter = new HashSet<String>();
        for (Job job : jobs) {
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
