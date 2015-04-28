/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "team_member")
  private String teamMember;

  public StudyTeamPK() {
  }

  public StudyTeamPK(String name, String teamMember) {
    this.name = name;
    this.teamMember = teamMember;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
    hash += (name != null ? name.hashCode() : 0);
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
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
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
    return "se.kth.bbc.study.StudyTeamPK[ name=" + name + ", teamMember="
            + teamMember + " ]";
  }

}
