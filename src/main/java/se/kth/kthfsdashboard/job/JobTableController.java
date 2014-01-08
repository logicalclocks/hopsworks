/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterEntity;

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

    public JobTableController() {
    }

    @PostConstruct
    public void init() {
        jobs = jobHistory.findAll();

        HashSet<Job> temp = new HashSet(jobs);
        jobNamesOptions =
                createFilterOptions(temp.toArray(new Job[temp.size()]));
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public SelectItem[] jobNamesAsOptions() {
        return jobNamesOptions;
    }

    private SelectItem[] createFilterOptions(Job[] data) {
        SelectItem[] options = new SelectItem[data.length + 1];

        options[0] = new SelectItem("", "Select");
        for (int i = 0; i < data.length; i++) {
            options[i + 1] = new SelectItem(data[i].getExecutedBy(), data[i].getExecutedBy());
        }

        return options;
    }

    public Job getSelectedJob() {
        return selectedJob;
    }

    public void setSelectedJob(Job selectedJob) {
        this.selectedJob = selectedJob;
    }

    public StreamedContent exportTable() {
        DefaultStreamedContent export = null;
        if (selectedJob != null) {
            String content = selectedJob.getTableJob();
            try {
                InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                export = new DefaultStreamedContent(stream, "text/csv", selectedJob.getName() + "-Table" + ".csv");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return export;
    }
}
