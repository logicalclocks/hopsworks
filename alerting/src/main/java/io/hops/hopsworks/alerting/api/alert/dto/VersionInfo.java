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
package io.hops.hopsworks.alerting.api.alert.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionInfo {
  private String branch;
  private String buildDate;
  private String buildUser;
  private String goVersion;
  private String revision;
  private String version;
  
  public VersionInfo() {
  }
  
  public String getBranch() {
    return branch;
  }
  
  public void setBranch(String branch) {
    this.branch = branch;
  }
  
  public String getBuildDate() {
    return buildDate;
  }
  
  public void setBuildDate(String buildDate) {
    this.buildDate = buildDate;
  }
  
  public String getBuildUser() {
    return buildUser;
  }
  
  public void setBuildUser(String buildUser) {
    this.buildUser = buildUser;
  }
  
  public String getGoVersion() {
    return goVersion;
  }
  
  public void setGoVersion(String goVersion) {
    this.goVersion = goVersion;
  }
  
  public String getRevision() {
    return revision;
  }
  
  public void setRevision(String revision) {
    this.revision = revision;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  @Override
  public String toString() {
    return "VersionInfo{" +
      "branch='" + branch + '\'' +
      ", buildDate=" + buildDate +
      ", buildUser='" + buildUser + '\'' +
      ", goVersion='" + goVersion + '\'' +
      ", revision='" + revision + '\'' +
      ", version='" + version + '\'' +
      '}';
  }
}
