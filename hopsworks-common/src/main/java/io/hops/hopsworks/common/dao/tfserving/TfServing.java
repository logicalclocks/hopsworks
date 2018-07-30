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

package io.hops.hopsworks.common.dao.tfserving;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "tf_serving", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TfServing.findAll", query = "SELECT t FROM TfServing t")
    , @NamedQuery(name = "TfServing.findById", query = "SELECT t FROM TfServing t WHERE t.id = :id")
    , @NamedQuery(name = "TfServing.findByProject", query = "SELECT t FROM TfServing t " +
        "WHERE t.project = :project")
    , @NamedQuery(name = "TfServing.findByHostIp", query = "SELECT t FROM TfServing t WHERE t.hostIp = :hostIp")
    , @NamedQuery(name = "TfServing.findByPort", query = "SELECT t FROM TfServing t WHERE t.port = :port")
    , @NamedQuery(name = "TfServing.findByPid", query = "SELECT t FROM TfServing t WHERE t.pid = :pid")
    , @NamedQuery(name = "TfServing.findByStatus", query = "SELECT t FROM TfServing t " +
        "WHERE t.status = :status")
    , @NamedQuery(name = "TfServing.findByHdfsUserId", query = "SELECT t FROM TfServing t " +
        "WHERE t.hdfsUserId = :hdfsUserId")
    , @NamedQuery(name = "TfServing.findByCreated", query = "SELECT t FROM TfServing t WHERE t.created = :created")
    , @NamedQuery(name = "TfServing.findByModelName", query = "SELECT t FROM TfServing t " +
        "WHERE t.modelName = :modelName")
    , @NamedQuery(name = "TfServing.findByHdfsModelPath", query = "SELECT t FROM TfServing t " +
        "WHERE t.hdfsModelPath = :hdfsModelPath")
    , @NamedQuery(name = "TfServing.findByVersion", query = "SELECT t FROM TfServing t WHERE t.version = :version")
    , @NamedQuery(name = "TfServing.findByEnableBatching", query = "SELECT t FROM TfServing t " +
        "WHERE t.enableBatching = :enableBatching")
    , @NamedQuery(name = "TfServing.updateRunningState", query = "UPDATE TfServing t SET t.status =" +
        " :status, t.pid = :pid, t.port = :port, t.hostIp = :hostIp WHERE t.id = :id")
    , @NamedQuery(name = "TfServing.updateModelVersion", query = "UPDATE TfServing t SET t.hdfsModelPath =" +
        " :hdfsModelPath, t.version = :version WHERE t.id = :id")})
public class TfServing implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 255)
  @Column(name = "host_ip")
  private String hostIp;
  @Column(name = "port")
  private Integer port;
  @Column(name = "pid")
  private BigInteger pid;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private TfServingStatusEnum status;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private int hdfsUserId;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "creator",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users creator;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "model_name")
  private String modelName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "hdfs_model_path")
  private String hdfsModelPath;
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private int version;
  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 255)
  @Column(name = "secret")
  private String secret;
  @Basic(optional = false)
  @NotNull
  @Column(name = "enable_batching")
  private boolean enableBatching;
  @Basic(optional = false)
  @NotNull
  @Column(name = "optimized")
  private boolean optimized;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public TfServing() {
  }

  public TfServing(Integer id) {
    this.id = id;
  }

  public TfServing(Integer id, TfServingStatusEnum status, int hdfsUserId, Date created,
                   String modelName, String hdfsModelPath, int version, boolean enableBatching, boolean optimized) {
    this.id = id;
    this.status = status;
    this.hdfsUserId = hdfsUserId;
    this.created = created;
    this.modelName = modelName;
    this.hdfsModelPath = hdfsModelPath;
    this.version = version;
    this.enableBatching = enableBatching;
    this.optimized = optimized;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public TfServingStatusEnum getStatus() {
    return status;
  }

  public void setStatus(TfServingStatusEnum status) {
    this.status = status;
  }

  public int getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(int hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getHdfsModelPath() {
    return hdfsModelPath;
  }

  public void setHdfsModelPath(String hdfsModelPath) {
    this.hdfsModelPath = hdfsModelPath;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public boolean getEnableBatching() {
    return enableBatching;
  }

  public void setEnableBatching(boolean enableBatching) {
    this.enableBatching = enableBatching;
  }

  public boolean isOptimized() {
    return optimized;
  }

  public void setOptimized(boolean optimized) {
    this.optimized = optimized;
  }
  
  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
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
    if (!(object instanceof TfServing)) {
      return false;
    }
    TfServing other = (TfServing) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.tfserving.TfServing[ id=" + id + " ]";
  }
    
}
