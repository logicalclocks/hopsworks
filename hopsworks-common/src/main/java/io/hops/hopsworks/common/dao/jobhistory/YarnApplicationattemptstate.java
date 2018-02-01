/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "yarn_applicationattemptstate",
        catalog = "hops",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnApplicationattemptstate.findAll",
          query = "SELECT y FROM YarnApplicationattemptstate y"),
  @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationid",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.yarnApplicationattemptstatePK.applicationid = :applicationid"),
  @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattemptid",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.yarnApplicationattemptstatePK.applicationattemptid = :applicationattemptid"),
  @NamedQuery(name
          = "YarnApplicationattemptstate.findByApplicationattempttrakingurl",
          query = "SELECT y FROM YarnApplicationattemptstate y "
          + "WHERE y.applicationattempttrakingurl = :applicationattempttrakingurl")})
public class YarnApplicationattemptstate implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected YarnApplicationattemptstatePK yarnApplicationattemptstatePK;
  @Lob
  @Column(name = "applicationattemptstate")
  private byte[] applicationattemptstate;
  @Size(max = 120)
  @Column(name = "applicationattempttrakingurl")
  private String applicationattempttrakingurl;

  @JoinColumn(name = "applicationid",
          referencedColumnName = "applicationid",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private YarnApplicationstate yarnApplicationstate;

  public YarnApplicationattemptstate() {
  }

  public YarnApplicationattemptstate(
          YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
    this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
  }

  public YarnApplicationattemptstate(String applicationid,
          String applicationattemptid) {
    this.yarnApplicationattemptstatePK = new YarnApplicationattemptstatePK(
            applicationid, applicationattemptid);
  }

  public YarnApplicationattemptstatePK getYarnApplicationattemptstatePK() {
    return yarnApplicationattemptstatePK;
  }

  public void setYarnApplicationattemptstatePK(
          YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
    this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
  }

  public byte[] getApplicationattemptstate() {
    return applicationattemptstate;
  }

  public void setApplicationattemptstate(byte[] applicationattemptstate) {
    this.applicationattemptstate = applicationattemptstate;
  }

  public String getApplicationattempttrakingurl() {
    return applicationattempttrakingurl;
  }

  public void setApplicationattempttrakingurl(
          String applicationattempttrakingurl) {
    this.applicationattempttrakingurl = applicationattempttrakingurl;
  }

  public YarnApplicationstate getYarnApplicationstate() {
    return yarnApplicationstate;
  }

  public void setYarnApplicationstate(YarnApplicationstate yarnApplicationstate) {
    this.yarnApplicationstate = yarnApplicationstate;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (yarnApplicationattemptstatePK != null
            ? yarnApplicationattemptstatePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnApplicationattemptstate)) {
      return false;
    }
    YarnApplicationattemptstate other = (YarnApplicationattemptstate) object;
    if ((this.yarnApplicationattemptstatePK == null
            && other.yarnApplicationattemptstatePK != null)
            || (this.yarnApplicationattemptstatePK != null
            && !this.yarnApplicationattemptstatePK.equals(
                    other.yarnApplicationattemptstatePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.yarn.YarnApplicationattemptstate[ yarnApplicationattemptstatePK="
            + yarnApplicationattemptstatePK + " ]";
  }

}
