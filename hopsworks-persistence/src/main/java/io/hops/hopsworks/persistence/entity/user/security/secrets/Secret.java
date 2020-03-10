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

package io.hops.hopsworks.persistence.entity.user.security.secrets;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "secrets", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "Secret.findByUser",
                query = "SELECT s FROM Secret s WHERE s.id.uid = :uid"),
    @NamedQuery(name = "Secret.findAll",
                query = "SELECT s FROM Secret s"),
    @NamedQuery(name = "Secret.findByName",
        query = "SELECT s FROM Secret s WHERE s.id.uid = :uid")
  })
public class Secret implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @EmbeddedId
  private SecretId id;
  
  @Column(name = "\"secret\"",
          nullable = false)
  private byte[] secret;
  
  @Column(name = "added_on",
          nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date addedOn;
  
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "visibility",
      nullable = false)
  private VisibilityType visibilityType;
  
  @Column(name = "pid_scope")
  private Integer projectIdScope;
  
  public Secret() {}
  
  public Secret(SecretId id, byte[] secret, Date addedOn) {
    this.id = id;
    this.secret = secret;
    this.addedOn = addedOn;
  }
  
  public SecretId getId() {
    return id;
  }
  
  public void setId(SecretId id) {
    this.id = id;
  }
  
  public byte[] getSecret() {
    return secret;
  }
  
  public void setSecret(byte[] secret) {
    this.secret = secret;
  }
  
  public Date getAddedOn() {
    return addedOn;
  }
  
  public void setAddedOn(Date addedOn) {
    this.addedOn = addedOn;
  }
  
  public VisibilityType getVisibilityType() {
    return visibilityType;
  }
  
  public void setVisibilityType(VisibilityType visibilityType) {
    this.visibilityType = visibilityType;
  }
  
  public Integer getProjectIdScope() {
    return projectIdScope;
  }
  
  public void setProjectIdScope(Integer projectIdScope) {
    this.projectIdScope = projectIdScope;
  }
}
