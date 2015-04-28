package se.kth.bbc.study;

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
public class StudyTeamPK implements Serializable {

  @Basic(optional = false)
  @Column(name = "study_id")
  private Integer studyId;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "team_member")
  private String teamMember;

  public StudyTeamPK() {
  }

  public StudyTeamPK(Integer studyId, String teamMember) {
    this.studyId = studyId;
    this.teamMember = teamMember;
  }

  public Integer getStudyId() {
    return studyId;
  }

  public void setStudyId(Integer studyId) {
    this.studyId = studyId;
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
    hash += (studyId != null ? studyId.hashCode() : 0);
    hash += (teamMember != null ? teamMember.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyTeamPK)) {
      return false;
    }
    StudyTeamPK other = (StudyTeamPK) object;
    if ((this.studyId == null && other.studyId != null) || (this.studyId != null
            && !this.studyId.equals(other.studyId))) {
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
    return "se.kth.bbc.study.StudyTeamPK[ studyId=" + studyId + ", teamMember="
            + teamMember + " ]";
  }

}
