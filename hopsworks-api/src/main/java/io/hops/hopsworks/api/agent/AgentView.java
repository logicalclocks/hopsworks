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
 *
 */

package io.hops.hopsworks.api.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.persistence.entity.command.SystemCommand;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Kagent communication protocol")
@XmlRootElement
public class AgentView {
  // Register
  @XmlElement(name = "host-id")
  @JsonAlias({"host-id"})
  private String hostId;
  private String password;
  private String hadoopHome;
  
  // Heartbeat
  @XmlElement(name = "agent-time")
  @JsonAlias({"agent-time"})
  private Long agentTime;
  @XmlElement(name = "num-gpus")
  @JsonAlias({"num-gpus"})
  private Integer numGpus;
  @XmlElement(name = "memory-capacity")
  @JsonAlias({"memory-capacity"})
  private Long memoryCapacity;
  private Integer cores;
  @XmlElement(name = "private-ip")
  @JsonAlias({"private-ip"})
  private String privateIp;

  private List<ServiceView> services;
  @XmlElement(name = "system-commands")
  @JsonAlias({"system-commands"})
  private List<SystemCommandView> systemCommands;
  private Boolean recover;
  
  public AgentView() {
  }
  
  @ApiModelProperty(value = "ID of the host kagent is running", required = true)
  public String getHostId() {
    return hostId;
  }
  
  public void setHostId(String hostId) {
    this.hostId = hostId;
  }
  
  @ApiModelProperty(value = "Password of kagent's REST API")
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  @ApiModelProperty(value = "Path to Hadoop home returned to agent after successful registration")
  public String getHadoopHome() {
    return hadoopHome;
  }
  
  public void setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
  }
  
  @ApiModelProperty(value = "Heartbeat timestamp", required = true)
  public Long getAgentTime() {
    return agentTime;
  }
  
  public void setAgentTime(Long agentTime) {
    this.agentTime = agentTime;
  }
  
  @ApiModelProperty(value = "Total memory capacity of host", required = true)
  public Long getMemoryCapacity() {
    return memoryCapacity;
  }
  
  public void setMemoryCapacity(Long memoryCapacity) {
    this.memoryCapacity = memoryCapacity;
  }
  
  @ApiModelProperty(value = "Number of available cores in host", required = true)
  public Integer getCores() {
    return cores;
  }
  
  public void setCores(Integer cores) {
    this.cores = cores;
  }
  
  @ApiModelProperty(value = "Private IP of host", required = true)
  public String getPrivateIp() {
    return privateIp;
  }
  
  public void setPrivateIp(String privateIp) {
    this.privateIp = privateIp;
  }
  
  @ApiModelProperty(value = "List of services running on host", dataType = "io.hops.hopsworks.api.agent.ServiceView")
  public List<ServiceView> getServices() {
    return services;
  }
  
  public void setServices(List<ServiceView> services) {
    this.services = services;
  }
  
  @ApiModelProperty(value = "ServiceStatus report of running system commands")
  public List<SystemCommandView> getSystemCommands() {
    return systemCommands;
  }
  
  public void setSystemCommands(List<SystemCommandView> systemCommands) {
    this.systemCommands = systemCommands;
  }
  
  @ApiModelProperty(value = "Flag to indicate if kagent needs to recover after a restart")
  private Boolean getRecover() {
    return recover;
  }
  
  private void setRecover(Boolean recover) {
    this.recover = recover;
  }
  
  public AgentController.AgentHeartbeatDTO toAgentHeartbeatDTO() {
    final List<AgentController.AgentServiceDTO> services = new ArrayList<>();
    if (this.services != null) {
      for (ServiceView sv : this.services) {
        services.add(sv.toAgentServiceDTO());
      }
    }
    
    final List<SystemCommand> systemCommands = new ArrayList<>();
    if (this.systemCommands != null) {
      for (final SystemCommandView scv : this.systemCommands) {
        systemCommands.add(scv.toSystemCommand());
      }
    }
    return new AgentController.AgentHeartbeatDTO(hostId, agentTime, numGpus, memoryCapacity,
        cores, privateIp, services, systemCommands, recover);
  }
  
  @Override
  public String toString() {
    return "Agent: " + hostId + " at time: " +
        (agentTime != null ? Instant.ofEpochMilli(agentTime).toString() : "unknown");
  }
}
