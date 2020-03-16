/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt;

import io.hops.hopsworks.persistence.entity.jwt.JwtSigningKey;

import java.util.Base64;
import java.util.Date;

public class DecryptedSigningKey {
  private Integer id;
  private String name;
  private Date createdOn;
  private byte[] decryptedSecret;
  
  public DecryptedSigningKey(JwtSigningKey jwtSigningKey, byte[] decryptedSecret) {
    this.id = jwtSigningKey.getId();
    this.name = jwtSigningKey.getName();
    this.createdOn = jwtSigningKey.getCreatedOn();
    this.decryptedSecret = decryptedSecret;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Date getCreatedOn() {
    return createdOn;
  }
  
  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }
  
  public byte[] getDecryptedSecret() {
    return decryptedSecret;
  }
  
  public void setDecryptedSecret(byte[] decryptedSecret) {
    this.decryptedSecret = decryptedSecret;
  }
  
  public String getDecryptedSecretBase64Encoded() {
    return Base64.getEncoder().encodeToString(decryptedSecret);
  }
}
