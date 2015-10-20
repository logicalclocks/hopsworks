package se.kth.bbc.project;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "hopsworks.project_team")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectTeam.findRoleForUserInProject",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.project = :project AND s.user = :user"),
  @NamedQuery(name = "ProjectTeam.findAll",
          query = "SELECT s FROM ProjectTeam s"),
  @NamedQuery(name = "ProjectTeam.findByProject",
          query = "SELECT s FROM ProjectTeam s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectTeam.findByTeamMember",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.user = :user"),
  @NamedQuery(name = "ProjectTeam.findByTeamRole",
          query = "SELECT s FROM ProjectTeam s WHERE s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.countStudiesByMember",
          query
          = "SELECT COUNT(s) FROM ProjectTeam s WHERE s.user = :user"),
  @NamedQuery(name = "ProjectTeam.countMembersForProjectAndRole",
          query
          = "SELECT COUNT(DISTINCT s.projectTeamPK.teamMember) FROM ProjectTeam s WHERE s.project=:project AND s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.countAllMembersForProject",
          query
          = "SELECT COUNT(DISTINCT s.projectTeamPK.teamMember) FROM ProjectTeam s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectTeam.findMembersByRoleInProject",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.project = :project AND s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.findAllMemberStudiesForUser",
          query
          = "SELECT st.project from ProjectTeam st WHERE st.user = :user"),
  @NamedQuery(name = "ProjectTeam.findAllJoinedStudiesForUser",
          query
          = "SELECT st.project from ProjectTeam st WHERE st.user = :user AND NOT st.project.owner = :user")})
public class ProjectTeam implements Serializable {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "team_member",
          referencedColumnName = "email",
          insertable
          = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Users user;

  @EmbeddedId
  protected ProjectTeamPK projectTeamPK;

  @Column(name = "team_role")
  private String teamRole;

  @Basic(optional = false)
  @NotNull
  @Column(name = "added")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  public ProjectTeam() {
  }

  public ProjectTeam(ProjectTeamPK projectTeamPK) {
    this.projectTeamPK = projectTeamPK;
  }

  public ProjectTeam(ProjectTeamPK projectTeamPK, Date timestamp) {
    this.projectTeamPK = projectTeamPK;
    this.timestamp = timestamp;
  }

  public ProjectTeam(Project project, Users user) {
    this.projectTeamPK = new ProjectTeamPK(project.getId(), user.getEmail());
  }

  public ProjectTeamPK getProjectTeamPK() {
    return projectTeamPK;
  }

  public void setProjectTeamPK(ProjectTeamPK projectTeamPK) {
    this.projectTeamPK = projectTeamPK;
  }

  public String getTeamRole() {
    return teamRole;
  }

  public void setTeamRole(String teamRole) {
    this.teamRole = teamRole;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectTeamPK != null ? projectTeamPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTeam)) {
      return false;
    }
    ProjectTeam other = (ProjectTeam) object;
    if ((this.projectTeamPK == null && other.projectTeamPK != null)
            || (this.projectTeamPK != null && !this.projectTeamPK.equals(
                    other.projectTeamPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.ProjectTeam[ projectTeamPK=" + projectTeamPK
            + " ]";
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

}
