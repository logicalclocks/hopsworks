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
  private String teamMember;

  public JupyterSettingsPK() {
  }

  public JupyterSettingsPK(int projectId, String teamMember) {
    this.projectId = projectId;
    this.teamMember = teamMember;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public String getTeamMember() {
    return teamMember;
  }

  public void setTeamMember(String teamMember) {
    this.teamMember = teamMember;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (teamMember != null ? teamMember.hashCode() : 0);
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
    if ((this.teamMember == null && other.teamMember != null) ||
            (this.teamMember != null && !this.teamMember.equals(other.teamMember))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterSettingsPK[ projectId=" +
            projectId + ", teamMember=" + teamMember + " ]";
  }

}
