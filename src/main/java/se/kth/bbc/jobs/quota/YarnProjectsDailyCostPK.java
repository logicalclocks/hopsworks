/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.quota;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author jdowling
 */
@Embeddable
public class YarnProjectsDailyCostPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "user")
  private String user;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 100)
  @Column(name = "projectname")
  private String projectname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "day")
  private long day;

  public YarnProjectsDailyCostPK() {
  }

  public YarnProjectsDailyCostPK(String user, String projectname, long day) {
    this.user = user;
    this.projectname = projectname;
    this.day = day;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getProjectname() {
    return projectname;
  }

  public void setProjectname(String projectname) {
    this.projectname = projectname;
  }

  public long getDay() {
    return day;
  }

  public void setDay(long day) {
    this.day = day;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (user != null ? user.hashCode() : 0);
    hash += (projectname != null ? projectname.hashCode() : 0);
    hash += (int) day;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnProjectsDailyCostPK)) {
      return false;
    }
    YarnProjectsDailyCostPK other = (YarnProjectsDailyCostPK) object;
    if ((this.user == null && other.user != null) || (this.user != null && !this.user.equals(other.user))) {
      return false;
    }
    if ((this.projectname == null && other.projectname != null) || (this.projectname != null && !this.projectname.equals(other.projectname))) {
      return false;
    }
    if (this.day != other.day) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.quota.YarnProjectsDailyCostPK[ user=" + user + ", projectname=" + projectname + ", day=" + day + " ]";
  }
  
}
