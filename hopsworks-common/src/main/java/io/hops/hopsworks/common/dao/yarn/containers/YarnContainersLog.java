/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.yarn.containers;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "yarn_containers_logs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnContainersLogs.findAll",
      query = "SELECT y FROM YarnContainersLogs y"),
  @NamedQuery(name = "YarnContainersLogs.findByContainerId",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.containerId = :containerId"),
  @NamedQuery(name = "YarnContainersLogs.findByStart",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.start = :start"),
  @NamedQuery(name = "YarnContainersLogs.findByStop",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.stop = :stop"),
  @NamedQuery(name = "YarnContainersLogs.findByExitStatus",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.exitStatus = :exitStatus"),
  @NamedQuery(name = "YarnContainersLogs.findByPrice",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.price = :price"),
  @NamedQuery(name = "YarnContainersLogs.findByVcores",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.vcores = :vcores"),
  @NamedQuery(name = "YarnContainersLogs.findByGpus",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.gpus = :gpus"),
  @NamedQuery(name = "YarnContainersLogs.findByMb",
      query = "SELECT y FROM YarnContainersLogs y WHERE y.mb = :mb"),
  @NamedQuery(name = "YarnContainersLogs.findByNodeId",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.nodeId = :nodeId"),
  @NamedQuery(name = "YarnContainersLogs.findRunningOnGpu",
      query
      = "SELECT y FROM YarnContainersLogs y WHERE y.gpus <> 0 and y.exitStatus = -201")})
public class YarnContainersLog implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "container_id")
  private String containerId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "start")
  private long start;
  @Column(name = "stop")
  private long stop;
  @Column(name = "exit_status")
  private Integer exitStatus;
  @Column(name = "price")
  private Float price;
  @Column(name = "vcores")
  private Integer vcores;
  @Column(name = "gpus")
  private Integer gpus;
  @Column(name = "mb")
  private long mb;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "node_id")
  private String nodeId;

  public YarnContainersLog() {
  }

  public YarnContainersLog(String containerId) {
    this.containerId = containerId;
  }

  public YarnContainersLog(String containerId, long start, String nodeId) {
    this.containerId = containerId;
    this.start = start;
    this.nodeId = nodeId;
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getStop() {
    return stop;
  }

  public void setStop(long stop) {
    this.stop = stop;
  }

  public Integer getExitStatus() {
    return exitStatus;
  }

  public void setExitStatus(Integer exitStatus) {
    this.exitStatus = exitStatus;
  }

  public Float getPrice() {
    return price;
  }

  public void setPrice(Float price) {
    this.price = price;
  }

  public Integer getVcores() {
    return vcores;
  }

  public void setVcores(Integer vcores) {
    this.vcores = vcores;
  }

  public Integer getGpus() {
    return gpus;
  }

  public void setGpus(Integer gpus) {
    this.gpus = gpus;
  }

  public long getMb() {
    return mb;
  }

  public void setMb(long mb) {
    this.mb = mb;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (containerId != null ? containerId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnContainersLog)) {
      return false;
    }
    YarnContainersLog other = (YarnContainersLog) object;
    if ((this.containerId == null && other.containerId != null) ||
        (this.containerId != null && !this.containerId.equals(other.containerId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.yarn.containers.YarnContainersLogs[ containerId=" + containerId + " ]";
  }
  
}
