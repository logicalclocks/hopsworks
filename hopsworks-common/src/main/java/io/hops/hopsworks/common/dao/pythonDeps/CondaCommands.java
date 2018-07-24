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

package io.hops.hopsworks.common.dao.pythonDeps;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "conda_commands",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "CondaCommands.findAll",
          query
          = "SELECT c FROM CondaCommands c"),
  @NamedQuery(name = "CondaCommands.findById",
          query
          = "SELECT c FROM CondaCommands c WHERE c.id = :id"),
  @NamedQuery(name = "CondaCommands.findByUser",
          query
          = "SELECT c FROM CondaCommands c WHERE c.user = :user"),
  @NamedQuery(name = "CondaCommands.findByOp",
          query
          = "SELECT c FROM CondaCommands c WHERE c.op = :op"),
  @NamedQuery(name = "CondaCommands.findByProj",
          query
          = "SELECT c FROM CondaCommands c WHERE c.projectId = :projectId"),
  @NamedQuery(name = "CondaCommands.findByChannelUrl",
          query
          = "SELECT c FROM CondaCommands c WHERE c.channelUrl = :channelUrl"),
  @NamedQuery(name = "CondaCommands.findByArg",
          query
          = "SELECT c FROM CondaCommands c WHERE c.arg = :arg"),
  @NamedQuery(name = "CondaCommands.findByLib",
          query
          = "SELECT c FROM CondaCommands c WHERE c.lib = :lib"),
  @NamedQuery(name = "CondaCommands.findByVersion",
          query
          = "SELECT c FROM CondaCommands c WHERE c.version = :version"),
  @NamedQuery(name = "CondaCommands.findByStatus",
          query
          = "SELECT c FROM CondaCommands c WHERE c.status = :status"),
  @NamedQuery(name = "CondaCommands.findByCreated",
          query
          = "SELECT c FROM CondaCommands c WHERE c.created = :created"),
  @NamedQuery(name = "CondaCommands.deleteAllFailedCommands",
          query
          = "DELETE FROM CondaCommands c WHERE c.status = :status"),
  @NamedQuery(name = "CondaCommands.findByHost",
          query = "SELECT c FROM CondaCommands c WHERE c.hostId = :host")})
public class CondaCommands implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 52)
  @Column(name = "user")
  private String user;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "proj")
  private String proj;
  @Size(max = 255)
  @Column(name = "channel_url")
  private String channelUrl;
  @Size(max = 255)
  @Column(name = "arg")
  private String arg="";
  @Size(max = 255)
  @Column(name = "lib")
  private String lib = "";
  @Size(max = 52)
  @Column(name = "version")
  private String version = "";
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 52)
  @Column(name = "op")
  @Enumerated(EnumType.STRING)
  private PythonDepsFacade.CondaOp op;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 52)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private PythonDepsFacade.CondaStatus status;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 52)
  @Column(name = "install_type")
  @Enumerated(EnumType.STRING)
  private PythonDepsFacade.CondaInstallType installType;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 52)
  @Column(name = "machine_type")
  @Enumerated(EnumType.STRING)
  private PythonDepsFacade.MachineType machineType;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
  @JoinColumn(name = "host_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Hosts hostId;
  @Size(min = 1,
          max = 10000)
  @Column(name = "environment_yml")
  private String environmentYml;

  public CondaCommands() {
  }

  public CondaCommands(Hosts h, String user, PythonDepsFacade.CondaOp op,
          PythonDepsFacade.CondaStatus status, PythonDepsFacade.CondaInstallType installType,
          PythonDepsFacade.MachineType machineType, Project project, String lib, String version, String channelUrl,
                       Date created, String arg,  String environmentYml) {
    this.hostId = h;
    if (op  == null || user == null || project == null) { 
      throw new NullPointerException("Op/user/project cannot be null");
    }
    this.user = user;
    this.op = op;
    this.proj = project.getName();
    this.projectId = project;
    this.status = status;
    this.installType = installType;
    this.machineType = machineType;
    this.created = created;
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
    this.arg = arg;
    this.environmentYml = environmentYml;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getProj() {
    return proj;
  }

  public void setProj(String proj) {
    this.proj = proj;
  }

  public String getChannelUrl() {
    return channelUrl;
  }

  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }

  public String getArg() {
    return arg;
  }

  public void setArg(String arg) {
    this.arg = arg;
  }

  public String getLib() {
    return lib;
  }

  public void setLib(String lib) {
    this.lib = lib;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }


  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  public Hosts getHostId() {
    return hostId;
  }

  public void setHostId(Hosts hostId) {
    this.hostId = hostId;
  }

  public PythonDepsFacade.CondaOp getOp() {
    return op;
  }

  public void setOp(PythonDepsFacade.CondaOp op) {
    this.op = op;
  }

  public PythonDepsFacade.CondaStatus getStatus() {
    return status;
  }

  public void setStatus(PythonDepsFacade.CondaStatus status) {
    this.status = status;
  }

  public PythonDepsFacade.CondaInstallType getInstallType() {
    return installType;
  }

  public void setInstallType(PythonDepsFacade.CondaInstallType installType) {
    this.installType = installType;
  }

  public PythonDepsFacade.MachineType getMachineType() {
    return machineType;
  }

  public void setMachineType(PythonDepsFacade.MachineType machineType) {
    this.machineType = machineType;
  }

  public String getEnvironmentYml() {
    return environmentYml;
  }

  public void setEnvironmentYml(String environmentYml) {
    this.environmentYml = environmentYml;
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
    if (!(object instanceof CondaCommands)) {
      return false;
    }
    CondaCommands other = (CondaCommands) object;
    if ((this.id == null && other.id != null) ||
            (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[ id=" + id + ", proj=" + proj  + ", op=" + op + ", installType=" + installType 
        + ", hostType=" + machineType + ", lib=" + lib + ", version=" + version + ", arg=" + arg 
        + ", channel=" + channelUrl + " ]";
  }

}
