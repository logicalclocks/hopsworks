/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "study_group_members")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StudyGroups.findAll",
          query = "SELECT s FROM StudyGroups s"),
  @NamedQuery(name = "StudyGroups.findByStudyname",
          query
          = "SELECT s FROM StudyGroups s WHERE s.studyGroupsPK.studyname = :studyname"),
  @NamedQuery(name = "StudyGroups.findByUsername",
          query
          = "SELECT s FROM StudyGroups s WHERE s.studyGroupsPK.username = :username"),
  @NamedQuery(name = "StudyGroups.findByTimeadded",
          query = "SELECT s FROM StudyGroups s WHERE s.timeadded = :timeadded"),
  @NamedQuery(name = "StudyGroups.findByAddedBy",
          query = "SELECT s FROM StudyGroups s WHERE s.addedBy = :addedBy"),
  @NamedQuery(name = "StudyGroups.findByTeamRole",
          query = "SELECT s FROM StudyGroups s WHERE s.teamRole = :teamRole")})
public class StudyGroups implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected StudyGroupsPK studyGroupsPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "timeadded")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timeadded;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "added_by")
  private String addedBy;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 10)
  @Column(name = "team_role")
  private String teamRole;
  @JoinColumn(name = "studyname",
          referencedColumnName = "name",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private TrackStudy trackStudy;

  public StudyGroups() {
  }

  public StudyGroups(StudyGroupsPK studyGroupsPK) {
    this.studyGroupsPK = studyGroupsPK;
  }

  public StudyGroups(StudyGroupsPK studyGroupsPK, Date timeadded, String addedBy,
          String teamRole) {
    this.studyGroupsPK = studyGroupsPK;
    this.timeadded = timeadded;
    this.addedBy = addedBy;
    this.teamRole = teamRole;
  }

  public StudyGroups(String studyname, String username) {
    this.studyGroupsPK = new StudyGroupsPK(studyname, username);
  }

  public StudyGroupsPK getStudyGroupsPK() {
    return studyGroupsPK;
  }

  public void setStudyGroupsPK(StudyGroupsPK studyGroupsPK) {
    this.studyGroupsPK = studyGroupsPK;
  }

  public Date getTimeadded() {
    return timeadded;
  }

  public void setTimeadded(Date timeadded) {
    this.timeadded = timeadded;
  }

  public String getAddedBy() {
    return addedBy;
  }

  public void setAddedBy(String addedBy) {
    this.addedBy = addedBy;
  }

  public String getTeamRole() {
    return teamRole;
  }

  public void setTeamRole(String teamRole) {
    this.teamRole = teamRole;
  }

  public TrackStudy getTrackStudy() {
    return trackStudy;
  }

  public void setTrackStudy(TrackStudy trackStudy) {
    this.trackStudy = trackStudy;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (studyGroupsPK != null ? studyGroupsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof StudyGroups)) {
      return false;
    }
    StudyGroups other = (StudyGroups) object;
    if ((this.studyGroupsPK == null && other.studyGroupsPK != null)
            || (this.studyGroupsPK != null && !this.studyGroupsPK.equals(
                    other.studyGroupsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.study.StudyGroups[ studyGroupsPK=" + studyGroupsPK + " ]";
  }

}
