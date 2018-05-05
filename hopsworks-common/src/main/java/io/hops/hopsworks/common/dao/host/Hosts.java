/*
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
 *
 */

package io.hops.hopsworks.common.dao.host;

import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.util.FormatUtils;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.hosts")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Hosts.find",
          query = "SELECT h FROM Hosts h"),
  @NamedQuery(name = "Hosts.findBy-Id",
          query = "SELECT h FROM Hosts h WHERE h.id = :id"),
  @NamedQuery(name = "Hosts.findBy-conda-enabled",
        query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true"),
  @NamedQuery(name = "Hosts.findBy-conda-enabled-gpu",
          query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true AND h.numGpus > 0"),
  @NamedQuery(name = "Hosts.findBy-conda-enabled-cpu",
        query = "SELECT h FROM Hosts h WHERE h.condaEnabled = true AND h.numGpus = 0"),
  @NamedQuery(name = "Hosts.findBy-Hostname",
          query = "SELECT h FROM Hosts h WHERE h.hostname = :hostname"),
  @NamedQuery(name = "Hosts.findBy-HostIp",
          query = "SELECT h FROM Hosts h WHERE h.hostIp = :hostIp"),
  @NamedQuery(name = "Hosts.findBy-Cluster.Group.Service.Status",
          query
          = "SELECT h FROM Hosts h, HostServices r WHERE h = r.host AND r.cluster "
          + "= :cluster AND r.group = :group AND r.service = :service AND r.status = :status"),
  @NamedQuery(name = "Hosts.findBy-Cluster.Group.Service",
          query
          = "SELECT h FROM Hosts h, HostServices r WHERE h = r.host AND r.cluster "
          + "= :cluster AND r.group = :group AND r.service = :service"),})
public class Hosts implements Serializable {

  private static final int HEARTBEAT_INTERVAL = 10;

  private static final long serialVersionUID = 1L;

  public static int getHeartbeatInterval(){
    return HEARTBEAT_INTERVAL;
  }
  
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

  @Column(name = "load1")
  private Double load1;

  @Column(name = "load5")
  private Double load5;

  @Column(name = "load15")
  private Double load15;

  @Column(name = "disk_capacity")
  private Long diskCapacity;

  @Column(name = "disk_used")
  private Long diskUsed;

  @Column(name = "memory_capacity")
  private Long memoryCapacity;

  @Column(name = "memory_used")
  private Long memoryUsed;

  @Column(name = "num_gpus")
  private int numGpus = 0;

  @Column(name = "registered")
  private boolean registered;

  @Column(name = "conda_enabled")
  private Boolean condaEnabled;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "hostId")
  private Collection<CondaCommands> condaCommandsCollection;

  @OneToMany(mappedBy = "host")
  private Collection<HostServices> hostServices;
  
  public Hosts() {
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

  public void setHostIp(String ip) {
    this.hostIp = ip;
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

  public Long getLastHeartbeat() {
    return lastHeartbeat;
  }

  public void setLastHeartbeat(long lastHeartbeat) {
    this.lastHeartbeat = lastHeartbeat;
  }

  public Integer getCores() {
    return cores;
  }

  public void setCores(int cores) {
    this.cores = cores;
  }

  public Double getLoad1() {
    return load1;
  }

  public void setLoad1(Double load1) {
    this.load1 = load1;
  }

  public Double getLoad5() {
    return load5;
  }

  public void setLoad5(Double load5) {
    this.load5 = load5;
  }

  public Double getLoad15() {
    return load15;
  }

  public void setLoad15(Double load15) {
    this.load15 = load15;
  }

  public Long getDiskCapacity() {
    return diskCapacity;
  }

  public void setDiskCapacity(Long diskCapacity) {
    this.diskCapacity = diskCapacity;
  }

  public Long getDiskUsed() {
    return diskUsed;
  }

  public void setDiskUsed(Long diskUsed) {
    this.diskUsed = diskUsed;
  }

  public Long getMemoryCapacity() {
    return memoryCapacity;
  }

  public void setMemoryCapacity(Long memoryCapacity) {
    this.memoryCapacity = memoryCapacity;
  }

  public Long getMemoryUsed() {
    return memoryUsed;
  }

  public void setMemoryUsed(Long memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  public int getNumGpus() {
    return numGpus;
  }

  public void setNumGpus(int numGpus) {
    this.numGpus = numGpus;
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

  public Boolean getCondaEnabled() { return condaEnabled; }

  public void setCondaEnabled(Boolean condaEnabled) { this.condaEnabled = condaEnabled; }

  public String getPublicOrPrivateIp() {
    // Prefer private IP, but return a public IP if the private ip is null
    if (publicIp == null || publicIp.isEmpty() || (publicIp != null && privateIp
            != null)) {
      return privateIp;
    }
    return publicIp;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getLastHeartbeatInSeconds() {

    DecimalFormat df = new DecimalFormat("#,###,##0.0");
    return df.format(((double) ((new Date()).getTime() - getLastHeartbeat()))
            / 1000);
  }

  public String getLastHeartbeatFormatted() {
    if (lastHeartbeat == null) {
      return "";
    }
    return FormatUtils.time(((new Date()).getTime() - lastHeartbeat));
  }

  public MemoryInfo getDiskInfo() {

    return new MemoryInfo(diskCapacity, diskUsed);
  }

  public MemoryInfo getMemoryInfo() {

    return new MemoryInfo(memoryCapacity, memoryUsed);
  }

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

  public Health getDiskHealth() {
    if (usagePercentage(diskUsed, diskCapacity) > 75) {
      return Health.Bad;
    }
    return Health.Good;
  }

  public Health getMemoryHealth() {
    if (usagePercentage(memoryUsed, memoryCapacity) > 85) {
      return Health.Bad;
    }
    return Health.Good;
  }

  public double usagePercentage(double used, double capacity) {
    return (used / capacity) * 100d;
  }

  public String getDiskPriority() {
    if (usagePercentage(diskUsed, diskCapacity) > 75) {
      return "priorityHigh";
    } else if (usagePercentage(diskUsed, diskCapacity) > 25) {
      return "priorityMed";
    }
    return "priorityLow";
  }

  public String getMemoryPriority() {
    if (usagePercentage(memoryUsed, memoryCapacity) > 75) {
      return "priorityHigh";
    } else if (usagePercentage(memoryUsed, memoryCapacity) > 25) {
      return "priorityMed";
    }
    return "priorityLow";
  }

  public String getDiskUsagePercentageString() {

    return String.format("%1.1f", usagePercentage(diskUsed, diskCapacity)) + "%";
  }

  public String getMemoryUsagePercentageString() {

    return String.format("%1.1f", usagePercentage(memoryUsed, memoryCapacity))
            + "%";
  }

  public String getDiskUsageInfo() {
    return FormatUtils.storage(diskUsed) + " / " + FormatUtils.storage(
            diskCapacity);
  }

  public String getMemoryUsageInfo() {
    return FormatUtils.storage(memoryUsed) + " / " + FormatUtils.storage(
            memoryCapacity);
  }

  @XmlTransient
  @JsonIgnore
  public Collection<CondaCommands> getCondaCommandsCollection() {
    return condaCommandsCollection;
  }

  public void setCondaCommandsCollection(
          Collection<CondaCommands> condaCommandsCollection) {
    this.condaCommandsCollection = condaCommandsCollection;
  }

  
  @XmlTransient
  @JsonIgnore
  public Collection<HostServices> getHostServicesCollection() {
    return hostServices;
  }

  public void setHostServicesCollection(Collection<HostServices> hostServicesCollection) {
    this.hostServices = hostServicesCollection;
  }

  @Override
  public String toString() {
    return this.hostIp + "(" + this.hostname + ")";
  }
}
