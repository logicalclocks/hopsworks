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
package io.hops.hopsworks.api.admin.services;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.host.ServiceStatus;
import io.hops.hopsworks.common.dao.kagent.HostServices;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServiceDTO extends RestDTO<ServiceDTO> {
  private Long id;
  private Integer hostId;
  private Integer pid;
  private String group;
  private String name;
  private ServiceStatus status;
  private Long uptime;
  private Long startTime;
  private Long stopTime;
  
  public ServiceDTO() {
  }
  
  public ServiceDTO(HostServices service) {
    this.setId(service.getId());
    this.setHostId(service.getHost().getId());
    this.setPid(service.getPid());
    this.setGroup(service.getGroup());
    this.setName(service.getName());
    this.setStatus(service.getStatus());
    this.setUptime(service.getUptime());
    this.setStartTime(service.getStartTime());
    this.setStopTime(service.getStopTime());
  }
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public Integer getHostId() {
    return hostId;
  }
  
  public void setHostId(Integer hostId) {
    this.hostId = hostId;
  }
  
  public Integer getPid() {
    return pid;
  }
  
  public void setPid(Integer pid) {
    this.pid = pid;
  }
  
  public String getGroup() {
    return group;
  }
  
  public void setGroup(String group) {
    this.group = group;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public ServiceStatus getStatus() {
    return status;
  }
  
  public void setStatus(ServiceStatus status) {
    this.status = status;
  }
  
  public Long getUptime() {
    return uptime;
  }
  
  public void setUptime(Long uptime) {
    this.uptime = uptime;
  }
  
  public Long getStartTime() {
    return startTime;
  }
  
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }
  
  public Long getStopTime() {
    return stopTime;
  }
  
  public void setStopTime(Long stopTime) {
    this.stopTime = stopTime;
  }
}
