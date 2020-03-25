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

package io.hops.hopsworks.persistence.entity.kagent;

import io.hops.hopsworks.persistence.entity.host.Health;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.host.ServiceStatus;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "host_services", catalog = "hopsworks")
@NamedQueries({
  @NamedQuery(name = "HostServices.findGroups",
    query = "SELECT DISTINCT r.group FROM HostServices r"),
  @NamedQuery(name = "HostServices.findByHostnameServiceNameGroup",
    query = "SELECT r FROM HostServices r WHERE r.group = :group AND r.name = :name AND r.host.hostname = :hostname"),
  @NamedQuery(name = "HostServices.findByHostname",
    query = "SELECT r FROM HostServices r WHERE r.host.hostname = :hostname ORDER BY r.group, r.name"),
  @NamedQuery(name = "HostServices.findByGroup",
    query = "SELECT r FROM HostServices r WHERE r.group = :group "),
  @NamedQuery(name = "HostServices.findByServiceName",
    query = "SELECT r FROM HostServices r WHERE r.name = :name"),
  @NamedQuery(name = "HostServices.CountServices",
    query = "SELECT COUNT(r) FROM HostServices r WHERE r.group = :group"),
  @NamedQuery(name = "HostServices.findByServiceNameAndHostname",
    query = "SELECT r FROM HostServices r WHERE r.host.hostname = :hostname AND r.name = :name")})
public class HostServices implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Column(name = "pid")
  private Integer pid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 48)
  @Column(name = "group_name")
  private String group;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 48)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "status")
  private ServiceStatus status;
  @Column(name = "uptime")
  private Long uptime;
  @Column(name = "startTime")
  private Long startTime;
  @Column(name = "stopTime")
  private Long stopTime;
  @JoinColumn(name = "host_id",
      referencedColumnName = "id")
  @ManyToOne
  private Hosts host;

  public HostServices() {
  }

  public HostServices(Long id) {
    this.id = id;
  }

  public HostServices(Long id, String name, String group, ServiceStatus status, Hosts host) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.status = status;
    this.host = host;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getPid() {
    return pid;
  }

  public void setPid(Integer pid) {
    this.pid = pid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
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

  public Hosts getHost() {
    return host;
  }

  public void setHost(Hosts host) {
    this.host = host;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HostServices)) {
      return false;
    }
    HostServices other = (HostServices) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Services[ id=" + id + " ]";
  }

  public Health getHealth() {
    if (status == ServiceStatus.Failed || status == ServiceStatus.Stopped) {
      return Health.Bad;
    }
    return Health.Good;
  }
}
