package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author stig
 */
@Embeddable
public class JobExecutionFilePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "job_id")
  private long jobId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;

  public JobExecutionFilePK() {
  }

  public JobExecutionFilePK(long jobId, String name) {
    this.jobId = jobId;
    this.name = name;
  }

  public long getJobId() {
    return jobId;
  }

  public void setJobId(long jobId) {
    this.jobId = jobId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) jobId;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobExecutionFilePK)) {
      return false;
    }
    JobExecutionFilePK other = (JobExecutionFilePK) object;
    if (this.jobId != other.jobId) {
      return false;
    }
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobExecutionFilePK[ jobId=" + jobId + ", name="
            + name + " ]";
  }

}
