package se.kth.bbc.study;

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
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "study_team")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyTeam.findRoleForUserInStudy",
          query
          = "SELECT s FROM StudyTeam s WHERE s.study = :study AND s.user = :user"),
  @NamedQuery(name = "StudyTeam.findAll",
          query = "SELECT s FROM StudyTeam s"),
  @NamedQuery(name = "StudyTeam.findByStudy",
          query = "SELECT s FROM StudyTeam s WHERE s.study = :study"),
  @NamedQuery(name = "StudyTeam.findByTeamMember",
          query
          = "SELECT s FROM StudyTeam s WHERE s.user = :user"),
  @NamedQuery(name = "StudyTeam.findByTeamRole",
          query = "SELECT s FROM StudyTeam s WHERE s.teamRole = :teamRole"),
  @NamedQuery(name = "StudyTeam.countStudiesByMember",
          query
          = "SELECT COUNT(s) FROM StudyTeam s WHERE s.user = :user"),
  @NamedQuery(name = "StudyTeam.countMembersForStudyAndRole",
          query
          = "SELECT COUNT(DISTINCT s.studyTeamPK.teamMember) FROM StudyTeam s WHERE s.study=:study AND s.teamRole = :teamRole"),
  @NamedQuery(name = "StudyTeam.countAllMembersForStudy",
          query
          = "SELECT COUNT(DISTINCT s.studyTeamPK.teamMember) FROM StudyTeam s WHERE s.study = :study"),
  @NamedQuery(name = "StudyTeam.findMembersByRoleInStudy",
          query
          = "SELECT s FROM StudyTeam s WHERE s.study = :study AND s.teamRole = :teamRole"),
  @NamedQuery(name = "StudyTeam.findAllMemberStudiesForUser",
          query
          = "SELECT st.study from StudyTeam st WHERE st.user = :user"),
  @NamedQuery(name = "StudyTeam.findAllJoinedStudiesForUser",
          query
          = "SELECT st.study from StudyTeam st WHERE st.user = :user AND NOT st.study.owner = :user")})
public class StudyTeam implements Serializable {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "study_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Study study;

  @JoinColumn(name = "team_member",
          referencedColumnName = "email",
          insertable
          = false,
          updatable = false)
  @ManyToOne(optional = false)
  private User user;

  @EmbeddedId
  protected StudyTeamPK studyTeamPK;

  @Column(name = "team_role")
  private String teamRole;

  @Basic(optional = false)
  @NotNull
  @Column(name = "added")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  public StudyTeam() {
  }

  public StudyTeam(StudyTeamPK studyTeamPK) {
    this.studyTeamPK = studyTeamPK;
  }

  public StudyTeam(StudyTeamPK studyTeamPK, Date timestamp) {
    this.studyTeamPK = studyTeamPK;
    this.timestamp = timestamp;
  }

  public StudyTeam(Study study, User user) {
    this.studyTeamPK = new StudyTeamPK(study.getId(), user.getEmail());
  }

  public StudyTeamPK getStudyTeamPK() {
    return studyTeamPK;
  }

  public void setStudyTeamPK(StudyTeamPK studyTeamPK) {
    this.studyTeamPK = studyTeamPK;
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
    hash += (studyTeamPK != null ? studyTeamPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyTeam)) {
      return false;
    }
    StudyTeam other = (StudyTeam) object;
    if ((this.studyTeamPK == null && other.studyTeamPK != null)
            || (this.studyTeamPK != null && !this.studyTeamPK.equals(
                    other.studyTeamPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.StudyTeam[ studyTeamPK=" + studyTeamPK + " ]";
  }

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
  }

  @XmlTransient
  @JsonIgnore
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

}
