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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "username",
  "password",
  "password_file"
  })
public class BasicAuth {
  @JsonProperty("username")
  private String username;
  @JsonProperty("password")
  private String password;
  @JsonProperty("password_file")
  private String passwordFile;
  
  public BasicAuth() {
  }
  
  public BasicAuth(String username) {
    this.username = username;
  }
  
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }
  
  @JsonProperty("username")
  public void setUsername(String username) {
    this.username = username;
  }
  
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }
  
  @JsonProperty("password")
  public void setPassword(String password) {
    this.password = password;
  }
  
  public BasicAuth withPassword(String password) {
    this.password = password;
    return this;
  }
  
  @JsonProperty("password_file")
  public String getPasswordFile() {
    return passwordFile;
  }
  
  @JsonProperty("password_file")
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
