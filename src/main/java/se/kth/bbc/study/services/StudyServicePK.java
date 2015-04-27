package se.kth.bbc.study.services;

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
public class StudyServicePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "study")
  private String study;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "service")
  private StudyServiceEnum service;

  public StudyServicePK() {
  }

  public StudyServicePK(String study, StudyServiceEnum service) {
    this.study = study;
    this.service = service;
  }

  public String getStudy() {
    return study;
  }

  public void setStudy(String study) {
    this.study = study;
  }

  public StudyServiceEnum getService() {
    return service;
  }

  public void setService(StudyServiceEnum service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (study != null ? study.hashCode() : 0);
    hash += (service != null ? service.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyServicePK)) {
      return false;
    }
    StudyServicePK other = (StudyServicePK) object;
    if ((this.study == null && other.study != null) || (this.study != null
            && !this.study.equals(other.study))) {
      return false;
    }
    if ((this.service == null && other.service != null) || (this.service != null
            && this.service != other.service)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.StudyServicePK[ study=" + study + ", service="
            + service + " ]";
  }

}
