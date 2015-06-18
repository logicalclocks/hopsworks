package se.kth.bbc.project.services;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

/**
 *
 * @author stig
 */
@Embeddable
public class ProjectServicePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "service")
  private ProjectServiceEnum service;

  public ProjectServicePK() {
  }

  public ProjectServicePK(int projectId, ProjectServiceEnum service) {
    this.projectId = projectId;
    this.service = service;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public ProjectServiceEnum getService() {
    return service;
  }

  public void setService(ProjectServiceEnum service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (service != null ? service.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectServicePK)) {
      return false;
    }
    ProjectServicePK other = (ProjectServicePK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.service == null && other.service != null) || (this.service != null
            && !this.service.equals(other.service))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[" + projectId + "," + service + "]";
  }

}
