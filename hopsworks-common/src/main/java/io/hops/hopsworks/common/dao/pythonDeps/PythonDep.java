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

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "python_dep",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "PythonDep.findAll",
          query
          = "SELECT p FROM PythonDep p"),
  @NamedQuery(name = "PythonDep.findById",
          query
          = "SELECT p FROM PythonDep p WHERE p.id = :id"),
  @NamedQuery(name = "PythonDep.findByDependency",
          query
          = "SELECT p FROM PythonDep p WHERE p.dependency = :dependency"),
  @NamedQuery(name = "PythonDep.findUniqueDependency",
          query
          = "SELECT p FROM PythonDep p WHERE p.dependency = :dependency AND p.version = :version " +
                  "AND p.installType = :installType AND p.repoUrl = :repoUrl AND p.machineType = :machineType " +
                  "AND p.status = :status"),
  @NamedQuery(name = "PythonDep.findByVersion",
          query
          = "SELECT p FROM PythonDep p WHERE p.version = :version")})
public class PythonDep implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "dependency")
  private String dependency;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "version")
  private String version;
  @Basic(optional = true)
  @Column(name = "preinstalled")
  private boolean preinstalled;

  @ManyToMany(mappedBy = "pythonDepCollection")
  private Collection<Project> projectCollection;
  @JoinColumn(name = "repo_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private AnacondaRepo repoUrl;

  @Column(name = "status")
  @Enumerated(EnumType.ORDINAL)
  private PythonDepsFacade.CondaStatus status
          = PythonDepsFacade.CondaStatus.NEW;

  @Column(name = "install_type")
  @Enumerated(EnumType.ORDINAL)
  private PythonDepsFacade.CondaInstallType installType;

  @Column(name = "machine_type")
  @Enumerated(EnumType.ORDINAL)
  private PythonDepsFacade.MachineType machineType;

  public PythonDep() {
  }

  public PythonDep(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDependency() {
    return dependency;
  }

  public void setDependency(String dependency) {
    this.dependency = dependency;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Project> getProjectCollection() {
    return projectCollection;
  }

  public void setProjectCollection(Collection<Project> projectCollection) {
    this.projectCollection = projectCollection;
  }

  public AnacondaRepo getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(AnacondaRepo repoUrl) {
    this.repoUrl = repoUrl;
  }

  public void setInstallType(PythonDepsFacade.CondaInstallType installType) {
    this.installType = installType;
  }

  public PythonDepsFacade.CondaInstallType getInstallType() {
    return installType;
  }

  public void setStatus(PythonDepsFacade.CondaStatus status) {
    this.status = status;
  }

  public PythonDepsFacade.CondaStatus getStatus() {
    return status;
  }

  public void setMachineType(PythonDepsFacade.MachineType machineType) {
    this.machineType = machineType;
  }

  public PythonDepsFacade.MachineType getMachineType() {
    return machineType;
  }

  public boolean isPreinstalled() {
    return preinstalled;
  }

  public void setPreinstalled(boolean preinstalled) {
    this.preinstalled = preinstalled;
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
    if (!(object instanceof PythonDep)) {
      return false;
    }
    PythonDep other = (PythonDep) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.pythonDeps.PythonDep[ id=" + id + " ]";
  }

}
