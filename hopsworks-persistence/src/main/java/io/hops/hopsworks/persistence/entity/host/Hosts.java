/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.persistence.entity.host;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.hops.hopsworks.persistence.entity.kagent.HostServices;
import io.hops.hopsworks.persistence.entity.util.FormatUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hosts", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Hosts.findByCondaEnabled",
        query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true"),
  @NamedQuery(name = "Hosts.findByCondaEnabledGpu",
          query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true AND h.numGpus > 0"),
  @NamedQuery(name = "Hosts.findByCondaEnabledCpu",
        query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true AND h.numGpus = 0"),
  @NamedQuery(name = "Hosts.findByHostname",
          query = "SELECT h FROM Hosts h WHERE h.hostname = :hostname"),
  @NamedQuery(name = "Hosts.findByHostIp",
          query = "SELECT h FROM Hosts h WHERE h.hostIp = :hostIp"),
  @NamedQuery(name = "Host.Count", query = "SELECT count(h.id) FROM Hosts h"),
  @NamedQuery(name = "Host.TotalCores", query = "SELECT SUM(h.cores) FROM Hosts h"),
  @NamedQuery(name = "Host.TotalGPUs", query = "SELECT SUM(h.numGpus) FROM Hosts h"),
  @NamedQuery(name = "Host.TotalMemoryCapacity", query = "SELECT SUM(h.memoryCapacity) FROM Hosts h")})
public class Hosts implements Serializable {

  private static final int HEARTBEAT_INTERVAL = 10;

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Column(name = "hostname",
          nullable = false,
          length = 128)
  private String hostname;

  @Column(name = "host_ip",
          nullable = false,
          length = 128)
  private String hostIp;

  @Column(name = "public_ip",
          length = 15)
  private String publicIp;

  @Column(name = "private_ip",
          length = 15)
  private String privateIp;

  @Column(name = "agent_password")
  private String agentPassword;

  @Column(name = "cores")
  private Integer cores;

  @Column(name = "last_heartbeat")
  private Long lastHeartbeat;

  @Column(name = "memory_capacity")
  private Long memoryCapacity;

  @Column(name = "num_gpus")
  private Integer numGpus;

  @Column(name = "registered")
  private Boolean registered;

  @Column(name = "conda_enabled")
  private Boolean condaEnabled;

  @OneToMany(mappedBy = "host")
  private Collection<HostServices> hostServices;
  
  public Hosts() {
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
  
  public Boolean getCondaEnabled() {
    return condaEnabled;
  }
  
  public void setCondaEnabled(Boolean condaEnabled) {
    this.condaEnabled = condaEnabled;
  }
  
  @JsonIgnore
  @XmlTransient
  public Collection<HostServices> getHostServices() {
    return hostServices;
  }
  
  public void setHostServices(Collection<HostServices> hostServices) {
    this.hostServices = hostServices;
  }
  
  // hosts.xhtml
  @JsonIgnore
  public String getPublicOrPrivateIp() {
    // Prefer private IP, but return a public IP if the private IP is null
    if (publicIp == null || publicIp.isEmpty()
        || (publicIp != null && privateIp != null)) {
      return privateIp;
    }
    return publicIp;
  }
  
  // hosts.xhtml
  @JsonIgnore
  public Health getHealth() {
    int hostTimeout = HEARTBEAT_INTERVAL * 2 + 1;
    if (lastHeartbeat == null) {
      return Health.Bad;
    }
    long deltaInSec = ((new Date()).getTime() - lastHeartbeat) / 1000;
    if (deltaInSec < hostTimeout) {
      return Health.Good;
    }
    return Health.Bad;
  }

  // hosts.xhtml
  @JsonIgnore
  public String getLastHeartbeatFormatted() {
    if (lastHeartbeat == null) {
      return "";
    }
    return FormatUtils.time(((new Date()).getTime() - lastHeartbeat));
  }

  @Override
  public String toString() {
    return this.hostIp + "(" + this.hostname + ")";
  }
}
