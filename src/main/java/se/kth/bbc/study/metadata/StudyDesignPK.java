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
public class StudyDesignPK implements Serializable {
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "study_id")
  private String studyId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "design")
  private CollectionTypeStudyDesignEnum design;

  public StudyDesignPK() {
  }

  public StudyDesignPK(String studyId, CollectionTypeStudyDesignEnum design) {
    this.studyId = studyId;
    this.design = design;
  }

  public String getStudyId() {
    return studyId;
  }

  public void setStudyId(String studyId) {
    this.studyId = studyId;
  }

  public CollectionTypeStudyDesignEnum getDesign() {
    return design;
  }

  public void setDesign(CollectionTypeStudyDesignEnum design) {
    this.design = design;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (studyId != null ? studyId.hashCode() : 0);
    hash += (design != null ? design.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyDesignPK)) {
      return false;
    }
    StudyDesignPK other = (StudyDesignPK) object;
    if ((this.studyId == null && other.studyId != null) ||
            (this.studyId != null && !this.studyId.equals(other.studyId))) {
      return false;
    }
    if ((this.design == null && other.design != null) ||
            (this.design != null && !this.design.equals(other.design))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyDesignPK[ studyId=" + studyId +
            ", design=" + design + " ]";
  }
  
}
