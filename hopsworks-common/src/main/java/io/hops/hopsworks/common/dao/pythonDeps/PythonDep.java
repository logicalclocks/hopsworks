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
  @NamedQuery(name = "PythonDep.findByDependencyAndVersion",
          query
          = "SELECT p FROM PythonDep p WHERE p.dependency = :dependency AND p.version = :version"),
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

  @Enumerated(EnumType.ORDINAL)
  private PythonDepsFacade.CondaStatus status
          = PythonDepsFacade.CondaStatus.ONGOING;

  public PythonDep() {
  }

  public PythonDep(Integer id) {
    this.id = id;
  }

  public PythonDep(String dependency, String version) {
    this.dependency = dependency;
    this.version = version;
  }

  public PythonDep(AnacondaRepo repoUrl, String dependency, String version) {
    this.dependency = dependency;
    this.version = version;
    this.repoUrl = repoUrl;
  }

  public PythonDep(AnacondaRepo repoUrl, String dependency, String version,
          boolean preinstalled) {
    this.dependency = dependency;
    this.version = version;
    this.repoUrl = repoUrl;
    this.preinstalled = preinstalled;
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

  public void setStatus(PythonDepsFacade.CondaStatus status) {
    this.status = status;
  }

  public PythonDepsFacade.CondaStatus getStatus() {
    return status;
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
