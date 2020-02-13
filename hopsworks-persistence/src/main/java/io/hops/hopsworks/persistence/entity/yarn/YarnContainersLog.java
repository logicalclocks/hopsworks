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

package io.hops.hopsworks.persistence.entity.yarn;

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
@Table(name = "yarn_containers_logs", catalog = "hops")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnContainersLog.findAll",
      query = "SELECT y FROM YarnContainersLog y"),
  @NamedQuery(name = "YarnContainersLog.findByContainerId",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.containerId = :containerId"),
  @NamedQuery(name = "YarnContainersLog.findByStart",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.start = :start"),
  @NamedQuery(name = "YarnContainersLog.findByStop",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.stop = :stop"),
  @NamedQuery(name = "YarnContainersLog.findByExitStatus",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.exitStatus = :exitStatus"),
  @NamedQuery(name = "YarnContainersLog.findByPrice",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.price = :price"),
  @NamedQuery(name = "YarnContainersLog.findByVcores",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.vcores = :vcores"),
  @NamedQuery(name = "YarnContainersLog.findByGpus",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.gpus = :gpus"),
  @NamedQuery(name = "YarnContainersLog.findByMb",
      query = "SELECT y FROM YarnContainersLog y WHERE y.mb = :mb"),
  @NamedQuery(name = "YarnContainersLog.findRunningOnGpu",
      query
      = "SELECT y FROM YarnContainersLog y WHERE y.gpus <> 0 and y.exitStatus = -201")})
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
  private Long start;
  @Column(name = "stop")
  private Long stop;
  @Column(name = "exit_status")
  private Integer exitStatus;
  @Column(name = "price")
  private Float price;
  @Column(name = "vcores")
  private Integer vcores;
  @Column(name = "gpus")
  private Integer gpus;
  @Column(name = "mb")
  private Long mb;

  public YarnContainersLog() {
  }

  public YarnContainersLog(String containerId) {
    this.containerId = containerId;
  }

  public YarnContainersLog(String containerId, long start) {
    this.containerId = containerId;
    this.start = start;
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public Long getStart() {
    return start;
  }

  public void setStart(Long start) {
    this.start = start;
  }

  public long getStop() {
    return stop;
  }

  public void setStop(Long stop) {
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

  public Long getMb() {
    return mb;
  }

  public void setMb(Long mb) {
    this.mb = mb;
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
    return "YarnContainersLogs[ containerId=" + containerId + " ]";
  }
  
}
