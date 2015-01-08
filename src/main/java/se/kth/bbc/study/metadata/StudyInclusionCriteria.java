package se.kth.bbc.study.metadata;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "STUDY_INCLUSION_CRITERIA")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyInclusionCriteria.findAll",
          query
          = "SELECT s FROM StudyInclusionCriteria s"),
  @NamedQuery(name = "StudyInclusionCriteria.findByStudyId",
          query
          = "SELECT s FROM StudyInclusionCriteria s WHERE s.studyInclusionCriteriaPK.studyId = :studyId"),
  @NamedQuery(name = "StudyInclusionCriteria.findByCriterium",
          query
          = "SELECT s FROM StudyInclusionCriteria s WHERE s.studyInclusionCriteriaPK.criterium = :criterium")})
public class StudyInclusionCriteria implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected StudyInclusionCriteriaPK studyInclusionCriteriaPK;
  @JoinColumn(name = "study_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private StudyMeta studyMeta;

  public StudyInclusionCriteria() {
  }

  public StudyInclusionCriteria(
          StudyInclusionCriteriaPK studyInclusionCriteriaPK) {
    this.studyInclusionCriteriaPK = studyInclusionCriteriaPK;
  }

  public StudyInclusionCriteria(String studyId, InclusionCriteriumEnum criterium) {
    this.studyInclusionCriteriaPK
            = new StudyInclusionCriteriaPK(studyId, criterium);
  }

  public StudyInclusionCriteriaPK getStudyInclusionCriteriaPK() {
    return studyInclusionCriteriaPK;
  }

  public void setStudyInclusionCriteriaPK(
          StudyInclusionCriteriaPK studyInclusionCriteriaPK) {
    this.studyInclusionCriteriaPK = studyInclusionCriteriaPK;
  }

  public StudyMeta getStudyMeta() {
    return studyMeta;
  }

  public void setStudyMeta(StudyMeta studyMeta) {
    this.studyMeta = studyMeta;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash +=
            (studyInclusionCriteriaPK != null
            ? studyInclusionCriteriaPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyInclusionCriteria)) {
      return false;
    }
    StudyInclusionCriteria other = (StudyInclusionCriteria) object;
    if ((this.studyInclusionCriteriaPK == null &&
            other.studyInclusionCriteriaPK != null) ||
            (this.studyInclusionCriteriaPK != null &&
            !this.studyInclusionCriteriaPK.equals(other.studyInclusionCriteriaPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyInclusionCriteria[ studyInclusionCriteriaPK=" +
            studyInclusionCriteriaPK + " ]";
  }
  
}
