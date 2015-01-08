package se.kth.bbc.study.metadata;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author stig
 */
@Embeddable
public class StudyInclusionCriteriaPK implements Serializable {
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "study_id")
  private String studyId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "criterium")
  private InclusionCriteriumEnum criterium;

  public StudyInclusionCriteriaPK() {
  }

  public StudyInclusionCriteriaPK(String studyId, InclusionCriteriumEnum criterium) {
    this.studyId = studyId;
    this.criterium = criterium;
  }

  public String getStudyId() {
    return studyId;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public InclusionCriteriumEnum getCriterium() {
    return criterium;
  }

  public void setCriterium(InclusionCriteriumEnum criterium) {
    this.criterium = criterium;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (studyId != null ? studyId.hashCode() : 0);
    hash += (criterium != null ? criterium.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyInclusionCriteriaPK)) {
      return false;
    }
    StudyInclusionCriteriaPK other = (StudyInclusionCriteriaPK) object;
    if ((this.studyId == null && other.studyId != null) ||
            (this.studyId != null && !this.studyId.equals(other.studyId))) {
      return false;
    }
    if ((this.criterium == null && other.criterium != null) ||
            (this.criterium != null && !this.criterium.equals(other.criterium))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyInclusionCriteriaPK[ studyId=" +
            studyId + ", criterium=" + criterium + " ]";
  }
  
}
