package se.kth.bbc.project;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@XmlRootElement
@Embeddable
public class ProjectTeamPK implements Serializable {

  @Basic(optional = false)
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "team_member")
  private String teamMember;

  public ProjectTeamPK() {
  }

  public ProjectTeamPK(Integer projectId, String teamMember) {
    this.projectId = projectId;
    this.teamMember = teamMember;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
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
    hash += (projectId != null ? projectId.hashCode() : 0);
    hash += (teamMember != null ? teamMember.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTeamPK)) {
      return false;
    }
    ProjectTeamPK other = (ProjectTeamPK) object;
    if ((this.projectId == null && other.projectId != null) || (this.projectId
            != null
            && !this.projectId.equals(other.projectId))) {
      return false;
    }
    if ((this.teamMember == null && other.teamMember != null)
            || (this.teamMember != null && !this.teamMember.equals(
                    other.teamMember))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.ProjectTeamPK[ projectId=" + projectId
            + ", teamMember="
            + teamMember + " ]";
  }

}
