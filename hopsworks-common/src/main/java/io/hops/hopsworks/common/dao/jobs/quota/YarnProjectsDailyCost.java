package io.hops.hopsworks.common.dao.jobs.quota;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.yarn_projects_daily_cost")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnProjectsDailyCost.findAll",
          query = "SELECT y FROM YarnProjectsDailyCost y"),
  @NamedQuery(name = "YarnProjectsDailyCost.findByUser",
          query
          = "SELECT y FROM YarnProjectsDailyCost y WHERE y.yarnProjectsDailyCostPK.user = :user"),
  @NamedQuery(name = "YarnProjectsDailyCost.findByProjectname",
          query
          = "SELECT y FROM YarnProjectsDailyCost y WHERE y.yarnProjectsDailyCostPK.projectname = :projectname"),
  @NamedQuery(name = "YarnProjectsDailyCost.findByDay",
          query
          = "SELECT y FROM YarnProjectsDailyCost y WHERE y.yarnProjectsDailyCostPK.day = :day"),
  @NamedQuery(name = "YarnProjectsDailyCost.findByCreditsUsed",
          query
          = "SELECT y FROM YarnProjectsDailyCost y WHERE y.creditsUsed = :creditsUsed")})
public class YarnProjectsDailyCost implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected YarnProjectsDailyCostPK yarnProjectsDailyCostPK;
  @Column(name = "credits_used")
  private Integer creditsUsed;

  public YarnProjectsDailyCost() {
  }

  public YarnProjectsDailyCost(YarnProjectsDailyCostPK yarnProjectsDailyCostPK) {
    this.yarnProjectsDailyCostPK = yarnProjectsDailyCostPK;
  }

  public YarnProjectsDailyCost(String user, String projectname, long day) {
    this.yarnProjectsDailyCostPK
            = new YarnProjectsDailyCostPK(user, projectname, day);
  }

  public YarnProjectsDailyCostPK getYarnProjectsDailyCostPK() {
    return yarnProjectsDailyCostPK;
  }

  public void setYarnProjectsDailyCostPK(
          YarnProjectsDailyCostPK yarnProjectsDailyCostPK) {
    this.yarnProjectsDailyCostPK = yarnProjectsDailyCostPK;
  }

  public Integer getCreditsUsed() {
    return creditsUsed;
  }

  public void setCreditsUsed(Integer creditsUsed) {
    this.creditsUsed = creditsUsed;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (yarnProjectsDailyCostPK != null ? yarnProjectsDailyCostPK.
            hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnProjectsDailyCost)) {
      return false;
    }
    YarnProjectsDailyCost other = (YarnProjectsDailyCost) object;
    if ((this.yarnProjectsDailyCostPK == null && other.yarnProjectsDailyCostPK
            != null) || (this.yarnProjectsDailyCostPK != null
            && !this.yarnProjectsDailyCostPK.equals(
                    other.yarnProjectsDailyCostPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.quota.YarnProjectsDailyCost[ yarnProjectsDailyCostPK="
            + yarnProjectsDailyCostPK + " ]";
  }

}
