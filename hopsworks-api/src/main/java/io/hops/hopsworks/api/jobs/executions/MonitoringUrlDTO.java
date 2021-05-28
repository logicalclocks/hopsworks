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
package io.hops.hopsworks.api.jobs.executions;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MonitoringUrlDTO {
  private String kibanaUrl;
  private String grafanaUrl;
  private String yarnUrl;
  private String tfUrl;
  private String sparkUrl;
  private String flinkMasterUrl;
  private String flinkHistoryServerUrl;
  
  public MonitoringUrlDTO() {
  }
  
  public String getKibanaUrl() {
    return kibanaUrl;
  }
  
  public void setKibanaUrl(String kibanaUrl) {
    this.kibanaUrl = kibanaUrl;
  }
  
  public String getGrafanaUrl() {
    return grafanaUrl;
  }
  
  public void setGrafanaUrl(String grafanaUrl) {
    this.grafanaUrl = grafanaUrl;
  }
  
  public String getYarnUrl() {
    return yarnUrl;
  }
  
  public void setYarnUrl(String yarnUrl) {
    this.yarnUrl = yarnUrl;
  }
  
  public String getTfUrl() {
    return tfUrl;
  }
  
  public void setTfUrl(String tfUrl) {
    this.tfUrl = tfUrl;
  }
  
  public String getSparkUrl() {
    return sparkUrl;
  }
  
  public void setSparkUrl(String sparkUrl) {
    this.sparkUrl = sparkUrl;
  }
  
  public String getFlinkMasterUrl() {
    return flinkMasterUrl;
  }
  
  public void setFlinkMasterUrl(String flinkMasterUrl) {
    this.flinkMasterUrl = flinkMasterUrl;
  }
  
  public String getFlinkHistoryServerUrl() {
    return flinkHistoryServerUrl;
  }
  
  public void setFlinkHistoryServerUrl(String flinkHistoryServerUrl) {
    this.flinkHistoryServerUrl = flinkHistoryServerUrl;
  }
}