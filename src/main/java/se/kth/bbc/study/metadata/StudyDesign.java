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
@Table(name = "STUDY_DESIGN")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyDesign.findAll",
          query
          = "SELECT s FROM StudyDesign s"),
  @NamedQuery(name = "StudyDesign.findByStudyId",
          query
          = "SELECT s FROM StudyDesign s WHERE s.studyDesignPK.studyId = :studyId"),
  @NamedQuery(name = "StudyDesign.findByDesign",
          query
          = "SELECT s FROM StudyDesign s WHERE s.studyDesignPK.design = :design")})
public class StudyDesign implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected StudyDesignPK studyDesignPK;
  @JoinColumn(name = "study_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private StudyMeta studyMeta;

  public StudyDesign() {
  }

  public StudyDesign(StudyDesignPK studyDesignPK) {
    this.studyDesignPK = studyDesignPK;
  }

  public StudyDesign(String studyId, CollectionTypeStudyDesignEnum design) {
    this.studyDesignPK = new StudyDesignPK(studyId, design);
  }

  public StudyDesignPK getStudyDesignPK() {
    return studyDesignPK;
  }

  public void setStudyDesignPK(StudyDesignPK studyDesignPK) {
    this.studyDesignPK = studyDesignPK;
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
    hash += (studyDesignPK != null ? studyDesignPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyDesign)) {
      return false;
    }
    StudyDesign other = (StudyDesign) object;
    if ((this.studyDesignPK == null && other.studyDesignPK != null) ||
            (this.studyDesignPK != null &&
            !this.studyDesignPK.equals(other.studyDesignPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.metadata.StudyDesign[ studyDesignPK=" +
            studyDesignPK + " ]";
  }
  
}
