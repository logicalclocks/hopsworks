/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ExecutionsInputfilesPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "execution_id")
  private int executionId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_pid")
  private int inodePid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "inode_name")
  private String inodeName;

  public ExecutionsInputfilesPK() {
  }

  public ExecutionsInputfilesPK(int executionId, int inodePid, String inodeName) {
    this.executionId = executionId;
    this.inodePid = inodePid;
    this.inodeName = inodeName;
  }

  public int getExecutionId() {
    return executionId;
  }

  public void setExecutionId(int executionId) {
    this.executionId = executionId;
  }

  public int getInodePid() {
    return inodePid;
  }

  public void setInodePid(int inodePid) {
    this.inodePid = inodePid;
  }

  public String getInodeName() {
    return inodeName;
  }

  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) executionId;
    hash += (int) inodePid;
    hash += (inodeName != null ? inodeName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ExecutionsInputfilesPK)) {
      return false;
    }
    ExecutionsInputfilesPK other = (ExecutionsInputfilesPK) object;
    if (this.executionId != other.executionId) {
      return false;
    }
    if (this.inodePid != other.inodePid) {
      return false;
    }
    if ((this.inodeName == null && other.inodeName != null) || (this.inodeName != null && !this.inodeName.equals(other.inodeName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.execution.ExecutionsInputfilesPK[ executionId=" + executionId + ", inodePid=" + inodePid + ", inodeName=" + inodeName + " ]";
  }
  
}
