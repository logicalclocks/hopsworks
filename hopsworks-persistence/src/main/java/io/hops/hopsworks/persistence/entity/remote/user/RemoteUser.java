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
package io.hops.hopsworks.persistence.entity.remote.user;

import io.hops.hopsworks.persistence.entity.user.Users;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "remote_user",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RemoteUser.findAll",
      query = "SELECT r FROM RemoteUser r")
  ,
    @NamedQuery(name = "RemoteUser.findById",
      query = "SELECT r FROM RemoteUser r WHERE r.id = :id")
  ,
    @NamedQuery(name = "RemoteUser.findByType",
      query = "SELECT r FROM RemoteUser r WHERE r.type = :type")
  ,
    @NamedQuery(name = "RemoteUser.findByAuthKey",
      query = "SELECT r FROM RemoteUser r WHERE r.authKey = :authKey")
  ,
    @NamedQuery(name = "RemoteUser.findByUuid",
      query = "SELECT r FROM RemoteUser r WHERE r.uuid = :uuid")
  ,
    @NamedQuery(name = "RemoteUser.findByUid",
      query = "SELECT r FROM RemoteUser r WHERE r.uid = :uid")})
public class RemoteUser implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "type")
  private RemoteUserType type;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 64)
  @Column(name = "auth_key")
  private String authKey;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "uuid")
  private String uuid;
  @JoinColumn(name = "uid",
      referencedColumnName = "uid")
  @OneToOne(optional = false,
      cascade = CascadeType.PERSIST)
  private Users uid;

  public RemoteUser() {
  }

  public RemoteUser(String uuid, Users uid, String authKey, RemoteUserType type) {
    this.type = type;
    this.authKey = authKey;
    this.uuid = uuid;
    this.uid = uid;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public RemoteUserType getType() {
    return type;
  }

  public void setType(RemoteUserType type) {
    this.type = type;
  }

  public String getAuthKey() {
    return authKey;
  }

  public void setAuthKey(String authKey) {
    this.authKey = authKey;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Users getUid() {
    return uid;
  }

  public void setUid(Users uid) {
    this.uid = uid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RemoteUser)) {
      return false;
    }
    RemoteUser other = (RemoteUser) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "RemoteUser[ id=" + id + " ]";
  }

}
