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
package io.hops.hopsworks.common.dao.host;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class HostDTO implements Serializable {
  private Integer id;
  private String hostname;
  private String hostIp;
  private String publicIp;
  private String privateIp;
  private String agentPassword;
  private Integer cores;
  private Long lastHeartbeat;
  private Long memoryCapacity;
  private Integer numGpus;
  private Boolean registered;
  
  public HostDTO() {
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getHostname() {
    return hostname;
  }
  
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
  
  public String getHostIp() {
    return hostIp;
  }
  
  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }
  
  public String getPublicIp() {
    return publicIp;
  }
  
  public void setPublicIp(String publicIp) {
    this.publicIp = publicIp;
  }
  
  public String getPrivateIp() {
    return privateIp;
  }
  
  public void setPrivateIp(String privateIp) {
    this.privateIp = privateIp;
  }
  
  public String getAgentPassword() {
    return agentPassword;
  }
  
  public void setAgentPassword(String agentPassword) {
    this.agentPassword = agentPassword;
  }
  
  public Integer getCores() {
    return cores;
  }
  
  public void setCores(Integer cores) {
    this.cores = cores;
  }
  
  public Long getLastHeartbeat() {
    return lastHeartbeat;
  }
  
  public void setLastHeartbeat(Long lastHeartbeat) {
    this.lastHeartbeat = lastHeartbeat;
  }
  
  public Long getMemoryCapacity() {
    return memoryCapacity;
  }
  
  public void setMemoryCapacity(Long memoryCapacity) {
    this.memoryCapacity = memoryCapacity;
  }
  
  public Integer getNumGpus() {
    return numGpus;
  }
  
  public void setNumGpus(Integer numGpus) {
    this.numGpus = numGpus;
  }
  
  public Boolean getRegistered() {
    return registered;
  }
  
  public void setRegistered(Boolean registered) {
    this.registered = registered;
  }

}
