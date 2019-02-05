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

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
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
  private String hostId;
  private String password;
  private String hadoopHome;
  
  // Heartbeat
  @XmlElement(name = "agent-time")
  private Long agentTime;
  private Double load1;
  private Double load5;
  private Double load15;
  @XmlElement(name = "num-gpus")
  private Integer numGpus;
  @XmlElement(name = "disk-used")
  private Long diskUsed;
  @XmlElement(name = "disk-capacity")
  private Long diskCapacity;
  @XmlElement(name = "memory-used")
  private Long memoryUsed;
  @XmlElement(name = "memory-capacity")
  private Long memoryCapacity;
  private Integer cores;
  @XmlElement(name = "private-ip")
  private String privateIp;
  @XmlElement(name = "services")
  private List<ServiceView> services;
  @XmlElement(name = "system-commands")
  private List<SystemCommandView> systemCommands;
  @XmlElement(name = "conda-commands")
  private List<CondaCommandView> condaCommands;
  @XmlElement(name = "conda-report")
  private List<String> condaReport;
  
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
  
  @ApiModelProperty(value = "load1 of host", required = true)
  public Double getLoad1() {
    return load1;
  }
  
  public void setLoad1(Double load1) {
    this.load1 = load1;
  }
  
  @ApiModelProperty(value = "load5 of host", required = true)
  public Double getLoad5() {
    return load5;
  }
  
  public void setLoad5(Double load5) {
    this.load5 = load5;
  }
  
  @ApiModelProperty(value = "load15 of host", required = true)
  public Double getLoad15() {
    return load15;
  }
  
  public void setLoad15(Double load15) {
    this.load15 = load15;
  }
  
  @ApiModelProperty(value = "Number of available GPUs in host", required = true)
  public Integer getNumGpus() {
    return numGpus;
  }
  
  public void setNumGpus(Integer numGpus) {
    this.numGpus = numGpus;
  }
  
  @ApiModelProperty(value = "Used disk space of host", required = true)
  public Long getDiskUsed() {
    return diskUsed;
  }
  
  public void setDiskUsed(Long diskUsed) {
    this.diskUsed = diskUsed;
  }
  
  @ApiModelProperty(value = "Total disk capacity of host", required = true)
  public Long getDiskCapacity() {
    return diskCapacity;
  }
  
  public void setDiskCapacity(Long diskCapacity) {
    this.diskCapacity = diskCapacity;
  }
  
  @ApiModelProperty(value = "Memory used in host", required = true)
  public Long getMemoryUsed() {
    return memoryUsed;
  }
  
  public void setMemoryUsed(Long memoryUsed) {
    this.memoryUsed = memoryUsed;
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
  
  @ApiModelProperty(value = "Status report of running system commands")
  public List<SystemCommandView> getSystemCommands() {
    return systemCommands;
  }
  
  public void setSystemCommands(List<SystemCommandView> systemCommands) {
    this.systemCommands = systemCommands;
  }
  
  @ApiModelProperty(value = "Status report of running conda commands")
  public List<CondaCommandView> getCondaCommands() {
    return condaCommands;
  }
  
  public void setCondaCommands(List<CondaCommandView> condaCommands) {
    this.condaCommands = condaCommands;
  }
  
  @ApiModelProperty(value = "List of Anaconda environments to check for garbage collection")
  public List<String> getCondaReport() {
    return condaReport;
  }
  
  public void setCondaReport(List<String> condaReport) {
    this.condaReport = condaReport;
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
    
    final List<CondaCommands> condaCommands = new ArrayList<>();
    if (this.condaCommands != null) {
      for (final CondaCommandView ccv : this.condaCommands) {
        condaCommands.add(ccv.toCondaCommands());
      }
    }
    
    return new AgentController.AgentHeartbeatDTO(hostId, agentTime, load1, load5, load15, numGpus, diskUsed,
        diskCapacity, memoryUsed, memoryCapacity, cores, privateIp, services, systemCommands, condaCommands,
        condaReport);
  }
  
  @Override
  public String toString() {
    return "Agent: " + hostId + " at time: " +
        (agentTime != null ? Instant.ofEpochMilli(agentTime).toString() : "unknown");
  }
}
