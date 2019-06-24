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

package io.hops.hopsworks.common.dao.user.security;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "users_third_party_api_keys", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "ThirdPartyApiKey.findByUser",
                query = "SELECT k FROM ThirdPartyApiKey k WHERE k.id.uid = :uid")
  })
public class ThirdPartyApiKey implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @EmbeddedId
  private ThirdPartyApiKeyId id;
  
  @Column(name = "\"key\"",
          nullable = false)
  private byte[] key;
  
  @Column(name = "added_on",
          nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date addedOn;
  
  public ThirdPartyApiKey() {}
  
  public ThirdPartyApiKey(ThirdPartyApiKeyId id, byte[] key, Date addedOn) {
    this.id = id;
    this.key = key;
    this.addedOn = addedOn;
  }
  
  public ThirdPartyApiKeyId getId() {
    return id;
  }
  
  public void setId(ThirdPartyApiKeyId id) {
    this.id = id;
  }
  
  public byte[] getKey() {
    return key;
  }
  
  public void setKey(byte[] key) {
    this.key = key;
  }
  
  public Date getAddedOn() {
    return addedOn;
  }
  
  public void setAddedOn(Date addedOn) {
    this.addedOn = addedOn;
  }
}
