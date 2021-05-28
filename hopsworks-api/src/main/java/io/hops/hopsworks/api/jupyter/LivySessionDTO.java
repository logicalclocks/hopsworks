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

package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.jobs.executions.MonitoringUrlDTO;
import io.hops.hopsworks.common.livy.LivyMsg;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LivySessionDTO {
  private int id;
  private String appId;
  private String kind;
  private String owner;
  private String proxyUser;
  private String state;
  private MonitoringUrlDTO monitoring;
  
  public LivySessionDTO() {
  }
  
  public LivySessionDTO(LivyMsg.Session livySession) {
    this.id = livySession.getId();
    this.appId = livySession.getAppId();
    this.kind = livySession.getKind();
    this.owner = livySession.getOwner();
    this.proxyUser = livySession.getProxyUser();
    this.state = livySession.getState();
  }
  
  public int getId() {
    return id;
  }
  
  public void setId(int id) {
    this.id = id;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public String getKind() {
    return kind;
  }
  
  public void setKind(String kind) {
    this.kind = kind;
  }
  
  public String getOwner() {
    return owner;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }
  
  public String getProxyUser() {
    return proxyUser;
  }
  
  public void setProxyUser(String proxyUser) {
    this.proxyUser = proxyUser;
  }
  
  public String getState() {
    return state;
  }
  
  public void setState(String state) {
    this.state = state;
  }
  
  public MonitoringUrlDTO getMonitoring() {
    return monitoring;
  }
  
  public void setMonitoring(MonitoringUrlDTO monitoring) {
    this.monitoring = monitoring;
  }
}