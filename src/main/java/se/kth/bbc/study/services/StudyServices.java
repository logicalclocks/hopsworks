package se.kth.bbc.study.services;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.study.Study;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "study_services")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyServices.findAll",
          query
          = "SELECT s FROM StudyServices s"),
  @NamedQuery(name = "StudyServices.findByStudy",
          query
          = "SELECT s FROM StudyServices s WHERE s.study = :study"),
  @NamedQuery(name = "StudyServices.findServicesByStudy",
          query
          = "SELECT s.studyServicePK.service FROM StudyServices s WHERE s.study = :study ORDER BY s.studyServicePK.service"),
  @NamedQuery(name = "StudyServices.findByService",
          query
          = "SELECT s FROM StudyServices s WHERE s.studyServicePK.service = :service")})
public class StudyServices implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected StudyServicePK studyServicePK;
  @JoinColumn(name = "study_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Study study;

  public StudyServices() {
  }

  public StudyServices(StudyServicePK studyServicesPK) {
    this.studyServicePK = studyServicesPK;
  }

  public StudyServices(Study study, StudyServiceEnum service) {
    this.studyServicePK = new StudyServicePK(study.getId(), service);
  }

  public StudyServicePK getStudyServicesPK() {
    return studyServicePK;
  }

  public void setStudyServicesPK(StudyServicePK studyServicesPK) {
    this.studyServicePK = studyServicesPK;
  }

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (studyServicePK != null ? studyServicePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyServices)) {
      return false;
    }
    StudyServices other = (StudyServices) object;
    if ((this.studyServicePK == null && other.studyServicePK != null)
            || (this.studyServicePK != null && !this.studyServicePK.equals(
                    other.studyServicePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[" + study + "," + studyServicePK.getService() + " ]";
  }

}
