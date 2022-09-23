/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.alerting.config.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicAuth {
  private String username;
  private String password;
  @JsonAlias({"password_file"})
  private String passwordFile;

  public BasicAuth() {
  }

  public BasicAuth(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public BasicAuth withPassword(String password) {
    this.password = password;
    return this;
  }

  public String getPasswordFile() {
    return passwordFile;
  }

  public void setPasswordFile(String passwordFile) {
    this.passwordFile = passwordFile;
  }

  public BasicAuth withPasswordFile(String passwordFile) {
    this.passwordFile = passwordFile;
    return this;
  }

  @Override
  public String toString() {
    return "BasicAuth{" +
      "username='" + username + '\'' +
      ", password='" + password + '\'' +
      ", passwordFile='" + passwordFile + '\'' +
      '}';
  }
}
