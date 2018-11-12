/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.host.Status;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@ApiModel(value = "Status report for running services on host")
@XmlRootElement
public class ServiceView {
  private String cluster;
  private String service;
  private String group;
  @XmlElement(name = "web-port")
  private String webPort;
  private Integer pid;
  private Status status;
  
  public ServiceView() {
  }
  
  @ApiModelProperty(value = "Name of the cluster", required = true)
  public String getCluster() {
    return cluster;
  }
  
  public void setCluster(String cluster) {
    this.cluster = cluster;
  }
  
  @ApiModelProperty(value = "Service name", required = true)
  public String getService() {
    return service;
  }
  
  public void setService(String service) {
    this.service = service;
  }
  
  @ApiModelProperty(value = "Name of the group service belongs to", required = true)
  public String getGroup() {
    return group;
  }
  
  public void setGroup(String group) {
    this.group = group;
  }
  
  @ApiModelProperty(value = "Service web port if available")
  public String getWebPort() {
    return webPort;
  }
  
  public void setWebPort(String webPort) {
    this.webPort = webPort;
  }
  
  @ApiModelProperty(value = "Process ID", required = true)
  public Integer getPid() {
    return pid;
  }
  
  public void setPid(Integer pid) {
    this.pid = pid;
  }
  
  @ApiModelProperty(value = "Current status of the service")
  public Status getStatus() {
    return status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }
  
  public AgentController.AgentServiceDTO toAgentServiceDTO() {
    return new AgentController.AgentServiceDTO(cluster, service, group, webPort, pid, status);
  }
}
