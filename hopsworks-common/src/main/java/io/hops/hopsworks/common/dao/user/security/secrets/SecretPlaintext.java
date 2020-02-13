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

package io.hops.hopsworks.common.dao.user.security.secrets;

import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;

import java.util.Date;

public final class SecretPlaintext {
  private final Users user;
  private final String keyName;
  private final Date addedOn;
  private String plaintext;
  private VisibilityType visibilityType;
  
  private SecretPlaintext(Users user, String keyName, String plaintext, Date addedOn, VisibilityType visibilityType) {
    this.user = user;
    this.keyName = keyName;
    this.plaintext = plaintext;
    this.addedOn = addedOn;
    this.visibilityType = visibilityType;
  }
  
  public static SecretPlaintext newInstance(Users user, String keyName, String plaintext, Date addedOn) {
    return new SecretPlaintext(user, keyName, plaintext, addedOn, null);
  }
  
  public static SecretPlaintext newInstance(Users user, String keyName, String plaintext, Date addedOn,
      VisibilityType visibilityType) {
    return new SecretPlaintext(user, keyName, plaintext, addedOn, visibilityType);
  }
  
  public Users getUser() {
    return user;
  }
  
  public String getKeyName() {
    return keyName;
  }
  
  public String getPlaintext() {
    return plaintext;
  }
  
  public void setPlaintext(String plaintext) {
    this.plaintext = plaintext;
  }
  
  public Date getAddedOn() {
    return addedOn;
  }
  
  public VisibilityType getVisibilityType() {
    return visibilityType;
  }
  
  public void setVisibilityType(VisibilityType visibilityType) {
    this.visibilityType = visibilityType;
  }
}
