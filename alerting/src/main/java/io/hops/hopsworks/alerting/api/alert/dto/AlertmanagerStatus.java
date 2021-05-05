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
import java.util.Date;

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertmanagerStatus {

  private ClusterStatus cluster;
  private AlertManagerConfig config;
  private Date uptime;
  private VersionInfo versionInfo;

  public AlertmanagerStatus() {
  }

  public ClusterStatus getCluster() {
    return cluster;
  }

  public void setCluster(ClusterStatus cluster) {
    this.cluster = cluster;
  }

  public AlertManagerConfig getConfig() {
    return config;
  }

  public void setConfig(AlertManagerConfig config) {
    this.config = config;
  }

  public Date getUptime() {
    return uptime;
  }

  public void setUptime(Date uptime) {
    this.uptime = uptime;
  }

  public VersionInfo getVersionInfo() {
    return versionInfo;
  }

  public void setVersionInfo(VersionInfo versionInfo) {
    this.versionInfo = versionInfo;
  }

  @Override
  public String toString() {
    return "AlertmanagerStatus{" +
      "cluster=" + cluster +
      ", config=" + config +
      ", uptime=" + uptime +
      ", versionInfo=" + versionInfo +
      '}';
  }
}
