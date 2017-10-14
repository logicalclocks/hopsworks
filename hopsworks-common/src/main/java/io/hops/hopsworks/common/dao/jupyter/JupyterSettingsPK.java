package io.hops.hopsworks.common.dao.jupyter;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class JupyterSettingsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 150)
  @Column(name = "team_member")
  private String email;

  public JupyterSettingsPK() {
  }

  public JupyterSettingsPK(int projectId, String email) {
    this.projectId = projectId;
    this.email = email;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String teamMember) {
    this.email = teamMember;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (email != null ? email.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JupyterSettingsPK)) {
      return false;
    }
    JupyterSettingsPK other = (JupyterSettingsPK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.email == null && other.email != null) ||
            (this.email != null && !this.email.equals(other.email))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterSettingsPK[ projectId=" +
            projectId + ", teamMember=" + email + " ]";
  }

}
