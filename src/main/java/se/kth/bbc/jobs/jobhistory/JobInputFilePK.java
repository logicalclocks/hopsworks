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
public class JobInputFilePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "execution_id")
  private long executionId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;

  public JobInputFilePK() {
  }

  public JobInputFilePK(long jobId, String name) {
    this.executionId = jobId;
    this.name = name;
  }

  public long getExecutionId() {
    return executionId;
  }

  public void setExecutionId(long executionId) {
    this.executionId = executionId;
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
    hash += (int) executionId;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobInputFilePK)) {
      return false;
    }
    JobInputFilePK other = (JobInputFilePK) object;
    if (this.executionId != other.executionId) {
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
    return "se.kth.bbc.job.JobInputFilePK[ jobId=" + executionId + ", name="
            + name
            + " ]";
  }

}
