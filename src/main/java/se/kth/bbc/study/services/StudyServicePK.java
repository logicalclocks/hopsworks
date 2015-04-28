package se.kth.bbc.study.services;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

/**
 *
 * @author stig
 */
@Embeddable
public class StudyServicePK implements Serializable {
  @Basic(optional = false)
  @NotNull
  @Column(name = "study_id")
  private int studyId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "service")
  private StudyServiceEnum service;

  public StudyServicePK() {
  }

  public StudyServicePK(int studyId, StudyServiceEnum service) {
    this.studyId = studyId;
    this.service = service;
  }

  public int getStudyId() {
    return studyId;
  }

  public void setStudyId(int studyId) {
    this.studyId = studyId;
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
    hash += (int) studyId;
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
    if (this.studyId != other.studyId) {
      return false;
    }
    if ((this.service == null && other.service != null) ||
            (this.service != null && !this.service.equals(other.service))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "["+studyId+","+service+"]";
  }
  
}
