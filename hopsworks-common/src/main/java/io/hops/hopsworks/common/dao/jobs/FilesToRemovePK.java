package io.hops.hopsworks.common.dao.jobs;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class FilesToRemovePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "execution_id")
  private long executionId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "filepath")
  private String filepath;

  public FilesToRemovePK() {
  }

  public FilesToRemovePK(long executionId, String path) {
    this.executionId = executionId;
    this.filepath = path;
  }

  public long getExecutionId() {
    return executionId;
  }

  public void setExecutionId(long executionId) {
    this.executionId = executionId;
  }

  public String getFilepath() {
    return filepath;
  }

  public void setFilepath(String filepath) {
    this.filepath = filepath;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) executionId;
    hash += (filepath != null ? filepath.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof FilesToRemovePK)) {
      return false;
    }
    FilesToRemovePK other = (FilesToRemovePK) object;
    if (this.executionId != other.executionId) {
      return false;
    }
    return this.filepath.equals(other.filepath);
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.FilesToRemovePK[ jobId=" + executionId + ", path=" + filepath + " ]";
  }

}
