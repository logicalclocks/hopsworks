/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;


@Entity
@Table(name = "jobs_history", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "JobsHistory.findAll", query = "SELECT j FROM JobsHistory j"),
    @NamedQuery(name = "JobsHistory.findByJobId", query = "SELECT j FROM JobsHistory j WHERE j.jobsHistoryPK.jobId = :jobId"),
    @NamedQuery(name = "JobsHistory.findByInodePid", query = "SELECT j FROM JobsHistory j WHERE j.jobsHistoryPK.inodePid = :inodePid"),
    @NamedQuery(name = "JobsHistory.findByInodeName", query = "SELECT j FROM JobsHistory j WHERE j.jobsHistoryPK.inodeName = :inodeName"),
    @NamedQuery(name = "JobsHistory.findByJobType", query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType"),
    @NamedQuery(name = "JobsHistory.findByClassName", query = "SELECT j FROM JobsHistory j WHERE j.className = :className"),
    @NamedQuery(name = "JobsHistory.findBySize", query = "SELECT j FROM JobsHistory j WHERE j.size = :size"),
    @NamedQuery(name = "JobsHistory.findByBlocksInHdfs", query = "SELECT j FROM JobsHistory j WHERE j.inputBlocksInHdfs = :inputBlocksInHdfs"),
    @NamedQuery(name = "JobsHistory.findByExecutionDuration", query = "SELECT j FROM JobsHistory j WHERE j.executionDuration = :executionDuration"),
    @NamedQuery(name = "JobsHistory.findByInitialRequestedMemory", query = "SELECT j FROM JobsHistory j WHERE j.initialRequestedMemory = :initialRequestedMemory"),
    @NamedQuery(name = "JobsHistory.findByInitialrequestedVcores", query = "SELECT j FROM JobsHistory j WHERE j.initialrequestedVcores = :initialrequestedVcores"),
    @NamedQuery(name = "JobsHistory.findByTotalRequestedMemory", query = "SELECT j FROM JobsHistory j WHERE j.totalRequestedMemory = :totalRequestedMemory"),
    @NamedQuery(name = "JobsHistory.findByTotalrequestedVcores", query = "SELECT j FROM JobsHistory j WHERE j.totalrequestedVcores = :totalrequestedVcores"),
    @NamedQuery(name = "JobsHistory.findByUniqueId", query = "SELECT j FROM JobsHistory j WHERE j.jobsHistoryPK.jobId = :jobId AND j.jobsHistoryPK.inodePid = :inodePid AND j.jobsHistoryPK.inodeName = :inodeName"),
    @NamedQuery(name = "JobsHistory.findWithVeryHighSimilarity", query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className AND j.arguments = :arguments AND j.jobsHistoryPK.inodeName = :inodeName AND j.inputBlocksInHdfs = :inputBlocksInHdfs AND j.appId IS NOT NULL"),
    @NamedQuery(name = "JobsHistory.findWithHighSimilarity", query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className AND j.arguments = :arguments AND j.jobsHistoryPK.inodeName = :inodeName AND j.appId IS NOT NULL"),
    @NamedQuery(name = "JobsHistory.findWithMediumSimilarity", query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className AND j.jobsHistoryPK.inodeName = :inodeName AND j.appId IS NOT NULL"),
    @NamedQuery(name = "JobsHistory.findWithLowSimilarity", query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className AND j.appId IS NOT NULL")
})
public class JobsHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @EmbeddedId
    protected JobsHistoryPK jobsHistoryPK;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "app_id")
    private String appId;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "job_type")
    private String jobType;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "class_name")
    private String className;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "size")
    private int size;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "arguments")
    private String arguments;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "input_blocks_in_hdfs")
    private String inputBlocksInHdfs;
    @Basic(optional = false)
    
    @Column(name = "execution_duration")
    private long executionDuration;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "initial_requested_memory")
    private int initialRequestedMemory;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "initial_requested_Vcores")
    private int initialrequestedVcores;
    
    @Basic(optional = false)
    @Column(name = "total_requested_memory")
    private int totalRequestedMemory;
    
    @Basic(optional = false)
    @Column(name = "total_requested_Vcores")
    private int totalrequestedVcores;

    public JobsHistory() {
    }

    public JobsHistory(JobsHistoryPK jobsHistoryPK) {
        this.jobsHistoryPK = jobsHistoryPK;
    }

    public JobsHistory(int jobId, int inodePid, String inodeName,int executionId, String appId, String jobType, 
                       int size, String inputBlocksInHdfs, String arguments, String className,
                       int initialRequestedMemory, int initialrequestedVcores) {
        this.jobsHistoryPK = new JobsHistoryPK(jobId, inodePid, inodeName, executionId);
        this.appId = appId;
        this.jobType = jobType;
        this.size = size;
        this.inputBlocksInHdfs = inputBlocksInHdfs;
        this.arguments = arguments;
        this.className = className;
        this.executionDuration = -1;
        this.initialRequestedMemory = initialRequestedMemory;
        this.initialrequestedVcores = initialrequestedVcores;
        this.totalRequestedMemory = 0;
        this.totalrequestedVcores = 0;
    }

    public JobsHistory(int jobId, int inodePid, String inodeName, int executionId) {
        this.jobsHistoryPK = new JobsHistoryPK(jobId, inodePid, inodeName, executionId);
    }

    public JobsHistoryPK getJobsHistoryPK() {
        return jobsHistoryPK;
    }

    public void setJobsHistoryPK(JobsHistoryPK jobsHistoryPK) {
        this.jobsHistoryPK = jobsHistoryPK;
    }
    
    public String getAppId(){
        return appId;
    }
    
    public void setAppId(String appId){
        this.appId = appId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getArguments() {
        return arguments;
    }

    public void setArguments(String argument) {
        this.arguments = argument;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getInputBlocksInHdfs() {
        return inputBlocksInHdfs;
    }

    public void setInputBlocksInHdfs(String inputBlocksInHdfs) {
        this.inputBlocksInHdfs = inputBlocksInHdfs;
    }

    public long getExecutionDuration() {
        return executionDuration;
    }

    public void setExecutionDuration(long executionDuration) {
        this.executionDuration = executionDuration;
    }

    public int getInitialRequestedMemory() {
        return initialRequestedMemory;
    }

    public void setInitialRequestedMemory(int initialRequestedMemory) {
        this.initialRequestedMemory = initialRequestedMemory;
    }

    public int getInitialrequestedVcores() {
        return initialrequestedVcores;
    }

    public void setInitialrequestedVcores(int initialrequestedVcores) {
        this.initialrequestedVcores = initialrequestedVcores;
    }

    public int getTotalRequestedMemory() {
        return totalRequestedMemory;
    }

    public void setTotalRequestedMemory(int totalRequestedMemory) {
        this.totalRequestedMemory = totalRequestedMemory;
    }

    public int getTotalrequestedVcores() {
        return totalrequestedVcores;
    }

    public void setTotalrequestedVcores(int totalrequestedVcores) {
        this.totalrequestedVcores = totalrequestedVcores;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (jobsHistoryPK != null ? jobsHistoryPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof JobsHistory)) {
            return false;
        }
        JobsHistory other = (JobsHistory) object;
        if ((this.jobsHistoryPK == null && other.jobsHistoryPK != null) || (this.jobsHistoryPK != null && !this.jobsHistoryPK.equals(other.jobsHistoryPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.jobhistory.JobsHistory[ jobsHistoryPK=" + jobsHistoryPK + " ]";
    }
    
}
