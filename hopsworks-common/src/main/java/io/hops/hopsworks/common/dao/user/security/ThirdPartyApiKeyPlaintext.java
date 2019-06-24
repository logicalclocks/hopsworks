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

import io.hops.hopsworks.common.dao.user.Users;

public final class ThirdPartyApiKeyPlaintext {
  private final Users user;
  private final String keyName;
  private String plaintextKey;
  
  private ThirdPartyApiKeyPlaintext(Users user, String keyName, String plaintextKey) {
    this.user = user;
    this.keyName = keyName;
    this.plaintextKey = plaintextKey;
  }
  
  public static ThirdPartyApiKeyPlaintext newInstance(Users user, String keyName, String plaintextKey) {
    return new ThirdPartyApiKeyPlaintext(user, keyName, plaintextKey);
  }
  
  public Users getUser() {
    return user;
  }
  
  public String getKeyName() {
    return keyName;
  }
  
  public String getPlaintextKey() {
    return plaintextKey;
  }
  
  public void setPlaintextKey(String plaintextKey) {
    this.plaintextKey = plaintextKey;
  }
}
