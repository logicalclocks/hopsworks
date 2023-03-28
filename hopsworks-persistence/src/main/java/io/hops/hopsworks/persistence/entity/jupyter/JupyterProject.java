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

package io.hops.hopsworks.persistence.entity.jupyter;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.io.Serializable;
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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "jupyter_project",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JupyterProject.findAll",
      query = "SELECT j FROM JupyterProject j"),
  @NamedQuery(name = "JupyterProject.findByPort",
      query = "SELECT j FROM JupyterProject j WHERE j.port = :port"),
  @NamedQuery(name = "JupyterProject.findByProjectUser",
      query = "SELECT j FROM JupyterProject j WHERE j.project = :project AND j.user = :user"),
  @NamedQuery(name = "JupyterProject.findByCreated",
      query = "SELECT j FROM JupyterProject j WHERE j.created = :created"),
  @NamedQuery(name = "JupyterProject.findByToken",
      query = "SELECT j FROM JupyterProject j WHERE j.token = :token"),
  @NamedQuery(name = "JupyterProject.findByCid",
      query = "SELECT j FROM JupyterProject j WHERE j.cid = :cid")})
public class JupyterProject implements Serializable {

  private static final long serialVersionUID = 1L;

  @Basic(optional = false)
  @NotNull
  @Column(name = "cid")
  private String cid;

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
  @Column(name = "expires")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expires;
  @Basic(optional = false)
  @NotNull
  @Size(min = 20, max = 64)
  @Column(name = "secret")
  private String secret;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "token")
  private String token;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @JoinColumn(name = "uid", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users user;
  @Basic(optional = false)
  @NotNull
  @Column(name = "no_limit")
  private boolean noLimit;

  @Transient
  private long minutesUntilExpiration;

  public JupyterProject() {
  }

  public JupyterProject(Project project, Users user, String secret, Integer port, String token,
                        String cid, Date expires, boolean noLimit) {
    this.project = project;
    this.user = user;
    this.secret = secret;
    this.port = port;
    this.created = new Date();
    this.expires = expires;
    this.token = token;
    this.cid = cid;
    this.noLimit = noLimit;
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

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getExpires() {
    return expires;
  }

  public void setExpires(Date expires) {
    this.expires = expires;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public String getCid() {
    return cid;
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  public long getMinutesUntilExpiration() {
    return this.minutesUntilExpiration;
  }

  public void setMinutesUntilExpiration(long minutesUntilExpiration) {
    this.minutesUntilExpiration = minutesUntilExpiration;
  }

  public boolean isNoLimit() {
    return this.noLimit;
  }

  public void setNoLimit(boolean noLimit) {
    this.noLimit = noLimit;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (port != null ? port.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof JupyterProject)) {
      return false;
    }
    JupyterProject other = (JupyterProject) object;
    if ((this.port == null && other.port != null) || (this.port != null
            && !this.port.equals(other.port))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.jupyter.JupyterProject[ port=" + port
            + " ]";
  }
}
