/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.dao.maggy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table(name = "maggy_driver",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "MaggyDriver.findAll",
      query = "SELECT m FROM MaggyDriver m")
  ,
    @NamedQuery(name = "MaggyDriver.findByAppId",
      query
      = "SELECT m FROM MaggyDriver m WHERE m.maggyDriverPK.appId = :appId")
  ,
    @NamedQuery(name = "MaggyDriver.findByHostIp",
      query = "SELECT m FROM MaggyDriver m WHERE m.hostIp = :hostIp")
  ,
    @NamedQuery(name = "MaggyDriver.findByPort",
      query
      = "SELECT m FROM MaggyDriver m WHERE m.maggyDriverPK.port = :port")
  ,
    @NamedQuery(name = "MaggyDriver.findBySecret",
      query = "SELECT m FROM MaggyDriver m WHERE m.secret = :secret")
  ,
    @NamedQuery(name = "MaggyDriver.findByCreated",
      query = "SELECT m FROM MaggyDriver m WHERE m.created = :created")})
public class MaggyDriver implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected MaggyDriverPK maggyDriverPK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "host_ip")
  private String hostIp;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "secret")
  private String secret;
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  public MaggyDriver() {
  }

  public MaggyDriver(MaggyDriverPK maggyDriverPK) {
    this.maggyDriverPK = maggyDriverPK;
  }

  public MaggyDriver(MaggyDriverPK maggyDriverPK, String hostIp, String secret) {
    this.maggyDriverPK = maggyDriverPK;
    this.hostIp = hostIp;
    this.secret = secret;
  }

  public MaggyDriver(String appId, int port) {
    this.maggyDriverPK = new MaggyDriverPK(appId, port);
  }

  public MaggyDriverPK getMaggyDriverPK() {
    return maggyDriverPK;
  }

  public void setMaggyDriverPK(MaggyDriverPK maggyDriverPK) {
    this.maggyDriverPK = maggyDriverPK;
  }

  public String getAppId() {
    return this.maggyDriverPK.getAppId();
  }
  
  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (maggyDriverPK != null ? maggyDriverPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof MaggyDriver)) {
      return false;
    }
    MaggyDriver other = (MaggyDriver) object;
    if ((this.maggyDriverPK == null && other.maggyDriverPK != null) ||
        (this.maggyDriverPK != null && !this.maggyDriverPK.equals(other.maggyDriverPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.maggy.MaggyDriver[ maggyDriverPK=" + maggyDriverPK + " ]";
  }
  
}
