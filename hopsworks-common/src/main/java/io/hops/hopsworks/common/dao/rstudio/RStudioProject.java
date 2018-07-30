/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved

 * Hopsworks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.rstudio;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "rstudio_project",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RStudioProject.findAll",
          query
          = "SELECT j FROM RStudioProject j"),
  @NamedQuery(name = "RStudioProject.findByPort",
          query
          = "SELECT j FROM RStudioProject j WHERE j.port = :port"),
  @NamedQuery(name = "RStudioProject.findByHdfsUserId",
          query
          = "SELECT j FROM RStudioProject j WHERE j.hdfsUserId = :hdfsUserId"),
  @NamedQuery(name = "RStudioProject.findByCreated",
          query
          = "SELECT j FROM RStudioProject j WHERE j.created = :created"),
  @NamedQuery(name = "RStudioProject.findByHostIp",
          query
          = "SELECT j FROM RStudioProject j WHERE j.hostIp = :hostIp"),
  @NamedQuery(name = "RStudioProject.findByToken",
          query
          = "SELECT j FROM RStudioProject j WHERE j.token = :token"),
  @NamedQuery(name = "RStudioProject.findByPid",
          query
          = "SELECT j FROM RStudioProject j WHERE j.pid = :pid")})
public class RStudioProject implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "pid")
  private long pid;

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "port")
  private Integer port;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Column(name = "last_accessed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastAccessed;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "host_ip")
  private String hostIp;
  @Basic(optional = false)
  @NotNull
  @Size(min = 20,
          max = 64)
  @Column(name = "secret")
  private String secret;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "token")
  private String token;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private int hdfsUserId;

  public RStudioProject() {
  }

  public RStudioProject(Project project, String secret, Integer port,
          int hdfsUserId, String hostIp, String token, Long pid) {
    this.projectId = project;
    this.secret = secret;
    this.port = port;
    this.hdfsUserId = hdfsUserId;
    this.created = Date.from(Instant.now());
    this.lastAccessed = Date.from(Instant.now());
    this.hostIp = hostIp;
    this.token = token;
    this.pid = pid;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
  
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
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

  public Date getLastAccessed() {
    return lastAccessed;
  }

  public void setLastAccessed(Date lastAccessed) {
    this.lastAccessed = lastAccessed;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }


  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (port != null ? port.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof RStudioProject)) {
      return false;
    }
    RStudioProject other = (RStudioProject) object;
    if ((this.port == null && other.port != null) || (this.port != null
            && !this.port.equals(other.port))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.rstudio.RStudioProject[ port=" + port
            + " ]";
  }

  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }


}
