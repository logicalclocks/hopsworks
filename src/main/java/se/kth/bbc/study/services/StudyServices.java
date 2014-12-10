/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.services;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

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
          = "SELECT s FROM StudyServices s WHERE s.studyServicePK.study = :study"),
  @NamedQuery(name = "StudyServices.findServicesByStudy",
          query
          = "SELECT s.studyServicePK.service FROM StudyServices s WHERE s.studyServicePK.study = :study ORDER BY s.studyServicePK.service"),
  @NamedQuery(name = "StudyServices.findByService",
          query
          = "SELECT s FROM StudyServices s WHERE s.studyServicePK.service = :service")})
public class StudyServices implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected StudyServicePK studyServicePK;

  public StudyServices() {
  }

  public StudyServices(StudyServicePK studyServicePK) {
    this.studyServicePK = studyServicePK;
  }

  public StudyServices(String study, String service) {
    this.studyServicePK = new StudyServicePK(study, service);
  }

  public StudyServicePK getStudyServicePK() {
    return studyServicePK;
  }

  public void setStudyServicePK(StudyServicePK studyServicePK) {
    this.studyServicePK = studyServicePK;
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
    if ((this.studyServicePK == null && other.studyServicePK != null) ||
            (this.studyServicePK != null &&
            !this.studyServicePK.equals(other.studyServicePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.StudyService[ studyServicePK=" + studyServicePK +
            " ]";
  }
  
}
