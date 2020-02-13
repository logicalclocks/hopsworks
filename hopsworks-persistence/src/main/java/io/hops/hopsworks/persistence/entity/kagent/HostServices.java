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
import io.hops.hopsworks.persistence.entity.host.Status;
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
  @NamedQuery(name = "HostServices.findAll", query = "SELECT r from HostServices r"),
  @NamedQuery(name = "HostServices.findGroups", query = "SELECT DISTINCT r.group FROM HostServices r"),
  @NamedQuery(name = "HostServices.find",
      query = "SELECT r FROM HostServices r WHERE r.group = :group "
      + "AND r.service = :service AND r.host.hostname = :hostname"),
  @NamedQuery(name = "HostServices.findOnHost",
      query = "SELECT r FROM HostServices r " +
          "WHERE r.group = :group AND r.service = :service AND r.host.hostname = :hostname"),
  @NamedQuery(name = "HostServices.findBy-Hostname",
      query = "SELECT r FROM HostServices r WHERE r.host.hostname = :hostname ORDER BY r.group, r.service"),
  @NamedQuery(name = "HostServices.findBy-Group", query = "SELECT r FROM HostServices r WHERE r.group = :group "),
  @NamedQuery(name = "HostServices.findBy-Group-Service",
      query = "SELECT r FROM HostServices r WHERE r.group = :group AND r.service = :service"),
  @NamedQuery(name = "HostServices.findBy-Service", query = "SELECT r FROM HostServices r WHERE r.service = :service"),
  @NamedQuery(name = "HostServices.Count",
      query = "SELECT COUNT(r) FROM HostServices r WHERE r.group = :group AND r.service = :service"),
  @NamedQuery(name = "HostServices.Count-services",
      query = "SELECT COUNT(r) FROM HostServices r WHERE r.group = :group"),
  @NamedQuery(name = "HostServices.DeleteBy-Hostname",
      query = "DELETE FROM HostServices r WHERE r.host.hostname = :hostname")})
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
  @Column(name = "service")
  private String service;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "status")
  private Status status;
  @Column(name = "uptime")
  private long uptime;
  @Column(name = "startTime")
  private long startTime;
  @Column(name = "stopTime")
  private long stopTime;
  @JoinColumn(name = "host_id",
      referencedColumnName = "id")
  @ManyToOne
  private Hosts host;

  public HostServices() {
  }

  public HostServices(Long id) {
    this.id = id;
  }

  public HostServices(Long id, String service, String group, Status status, Hosts host) {
    this.id = id;
    this.service = service;
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

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getUptime() {
    return uptime;
  }

  public void setUptime(long uptime) {
    this.uptime = uptime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getStopTime() {
    return stopTime;
  }

  public void setStopTime(long stopTime) {
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
    if (status == Status.Failed || status == Status.Stopped) {
      return Health.Bad;
    }
    return Health.Good;
  }
}
