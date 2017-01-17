/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hopsworks.common.dao.pythonDeps;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author jdowling
 */
@Embeddable
public class ProjectPythondepsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "dep_id")
  private int depId;

  public ProjectPythondepsPK() {
  }

  public ProjectPythondepsPK(int projectId, int depId) {
    this.projectId = projectId;
    this.depId = depId;
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

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (int) depId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectPythondepsPK)) {
      return false;
    }
    ProjectPythondepsPK other = (ProjectPythondepsPK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if (this.depId != other.depId) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.ProjectPythondepsPK[ projectId=" + projectId + ", depId=" + depId + " ]";
  }
  
}
