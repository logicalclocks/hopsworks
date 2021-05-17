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
import com.google.common.base.Strings;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "id",
  "name",
  "username",
  "type"
  })
public class Responder {
  @JsonProperty("id")
  private String id;
  @JsonProperty("name")
  private String name;
  @JsonProperty("username")
  private String username;
  @JsonProperty("type")
  private String type;
  
  public Responder() {
  }
  
  public Responder(String id, String name, String username) {
    this.id = id;
    this.name = name;
    this.username = username;
    if (!Strings.isNullOrEmpty(id) && (!Strings.isNullOrEmpty(name) || !Strings.isNullOrEmpty(username))) {
      throw new IllegalStateException("Exactly one of id, name or username fields should be defined.");
    }
    if (!Strings.isNullOrEmpty(name) && (!Strings.isNullOrEmpty(id) || !Strings.isNullOrEmpty(username))) {
      throw new IllegalStateException("Exactly one of id, name or username fields should be defined.");
    }
    if (!Strings.isNullOrEmpty(username) && (!Strings.isNullOrEmpty(name) || !Strings.isNullOrEmpty(id))) {
      throw new IllegalStateException("Exactly one of id, name or username fields should be defined.");
    }
  }
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  
  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }
  
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }
  
  @JsonProperty("username")
  public void setUsername(String username) {
    this.username = username;
  }
  
  @JsonProperty("type")
  public String getType() {
    return type;
  }
  
  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }
  
  public Responder withType(String type) {
    this.type = type;
    return this;
  }
  
  @Override
  public String toString() {
    return "Responder{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", username='" + username + '\'' +
      ", type='" + type + '\'' +
      '}';
  }
}
