/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt.dao;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.invalid_jwt")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "InvalidJwt.findAll",
      query = "SELECT i FROM InvalidJwt i")
  ,
    @NamedQuery(name = "InvalidJwt.findAllJti",
      query = "SELECT i.jti FROM InvalidJwt i")
  ,
    @NamedQuery(name = "InvalidJwt.findByJti",
      query = "SELECT i FROM InvalidJwt i WHERE i.jti = :jti")
  ,
    @NamedQuery(name = "InvalidJwt.findByExpirationTime",
      query
      = "SELECT i FROM InvalidJwt i WHERE i.expirationTime = :expirationTime")
  ,
    @NamedQuery(name = "InvalidJwt.findExpired",
      query
      = "SELECT i FROM InvalidJwt i WHERE i.expirationTime < CURRENT_TIMESTAMP")
  ,
    @NamedQuery(name = "InvalidJwt.findByRenewableForSec",
      query
      = "SELECT i FROM InvalidJwt i WHERE i.renewableForSec = :renewableForSec")})
public class InvalidJwt implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "jti")
  private String jti;
  @Basic(optional = false)
  @NotNull
  @Column(name = "expiration_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationTime;
  @Basic(optional = false)
  @NotNull
  @Column(name = "renewable_for_sec")
  private int renewableForSec;

  public InvalidJwt() {
  }

  public InvalidJwt(String jti) {
    this.jti = jti;
  }

  public InvalidJwt(String jti, Date expirationTime, int renewableForSec) {
    this.jti = jti;
    this.expirationTime = expirationTime;
    this.renewableForSec = renewableForSec;
  }

  public String getJti() {
    return jti;
  }

  public void setJti(String jti) {
    this.jti = jti;
  }

  public Date getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(Date expirationTime) {
    this.expirationTime = expirationTime;
  }

  public int getRenewableForSec() {
    return renewableForSec;
  }

  public void setRenewableForSec(int renewableForSec) {
    this.renewableForSec = renewableForSec;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jti != null ? jti.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof InvalidJwt)) {
      return false;
    }
    InvalidJwt other = (InvalidJwt) object;
    if ((this.jti == null && other.jti != null) || (this.jti != null && !this.jti.equals(other.jti))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.jwt.dao.InvalidJwt[ jti=" + jti + " ]";
  }

}
