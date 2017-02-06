package io.hops.hopsworks.common.dao.pythonDeps;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class PythonDepHostStatusPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "dep_id")
  private int depId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "repo_id")
  private int repoId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "host_id")
  private int hostId;


  public PythonDepHostStatusPK() {
  }

  public PythonDepHostStatusPK(int projectId, int depId, int repoId, int hostId) {
    this.projectId = projectId;
    this.depId = depId;
    this.repoId = repoId;
    this.hostId = hostId;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public int getDepId() {
    return depId;
  }

  public void setDepId(int depId) {
    this.depId = depId;
  }

  public int getRepoId() {
    return repoId;
  }

  public void setRepoId(int repoId) {
    this.repoId = repoId;
  }

  public int getHostId() {
    return hostId;
  }

  public void setHostId(int hostId) {
    this.hostId = hostId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (int) depId;
    hash += (int) repoId;
    hash += (int) hostId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof PythonDepHostStatusPK)) {
      return false;
    }
    PythonDepHostStatusPK other = (PythonDepHostStatusPK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if (this.depId != other.depId) {
      return false;
    }
    if (this.repoId != other.repoId) {
      return false;
    }
    if (this.hostId != other.hostId) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.PythonDepHostStatusPK[ projectId="
            + projectId + ", depId=" + depId + ", repoId=" + repoId
            + ", hostId=" + hostId + " ]";
  }

}
