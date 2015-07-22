package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "hopsworks.job_execution_files")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobExecutionFile.findAll",
          query
          = "SELECT j FROM JobExecutionFile j"),
  @NamedQuery(name = "JobExecutionFile.findByJobId",
          query
          = "SELECT j FROM JobExecutionFile j WHERE j.jobExecutionFilePK.jobId = :jobId"),
  @NamedQuery(name = "JobExecutionFile.findByName",
          query
          = "SELECT j FROM JobExecutionFile j WHERE j.jobExecutionFilePK.name = :name"),
  @NamedQuery(name = "JobExecutionFile.findByPath",
          query
          = "SELECT j FROM JobExecutionFile j WHERE j.path = :path")})
public class JobExecutionFile implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected JobExecutionFilePK jobExecutionFilePK;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "path")
  private String path;

  @JoinColumn(name = "job_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Job job;

  public JobExecutionFile() {
  }

  public JobExecutionFile(JobExecutionFilePK jobExecutionFilePK) {
    this.jobExecutionFilePK = jobExecutionFilePK;
  }

  public JobExecutionFile(JobExecutionFilePK jobExecutionFilePK, String path) {
    this.jobExecutionFilePK = jobExecutionFilePK;
    this.path = path;
  }

  public JobExecutionFile(long jobId, String name) {
    this.jobExecutionFilePK = new JobExecutionFilePK(jobId, name);
  }

  public JobExecutionFilePK getJobExecutionFilePK() {
    return jobExecutionFilePK;
  }

  public void setJobExecutionFilePK(JobExecutionFilePK jobExecutionFilePK) {
    this.jobExecutionFilePK = jobExecutionFilePK;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Job getJob() {
    return job;
  }

  public void setJob(Job jobHistory) {
    this.job = jobHistory;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jobExecutionFilePK != null ? jobExecutionFilePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobExecutionFile)) {
      return false;
    }
    JobExecutionFile other = (JobExecutionFile) object;
    if ((this.jobExecutionFilePK == null && other.jobExecutionFilePK != null)
            || (this.jobExecutionFilePK != null && !this.jobExecutionFilePK.
            equals(other.jobExecutionFilePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobExecutionFile[ jobExecutionFilePK="
            + jobExecutionFilePK + " ]";
  }

}
