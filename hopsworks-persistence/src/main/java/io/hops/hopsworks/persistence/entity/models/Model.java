/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
 */

package io.hops.hopsworks.persistence.entity.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Description of the model.
 */
@Entity
@Table(name = "model", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Model.findAll",
        query = "SELECT m FROM Model m"),
    @NamedQuery(name = "Model.findByProjectAndName",
        query
            = "SELECT m FROM Model m WHERE m.name = :name AND m.project = :project"),
    @NamedQuery(name = "Model.findByProjectIdAndName",
        query
            = "SELECT m FROM Model m WHERE m.name = :name AND m.project.id = :projectId"),})
public class Model implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Size(max = 255)
  @Column(name = "name")
  private String name;

  @JoinColumn(name = "project_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  @OneToMany(mappedBy = "model")
  private Collection<ModelVersion> versions;

  @JsonIgnore
  @XmlTransient
  public Collection<ModelVersion> getVersions() {
    return versions;
  }

  public void setVersions(Collection<ModelVersion> versions) {
    this.versions = versions;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += getProject().getId();
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public final boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Model)) {
      return false;
    }
    Model other = (Model) object;
    if (!Objects.equals(this.project.getId(), other.getProject().getId())) {
      return false;
    }
    if ((this.name == null && other.name != null) ||
      (this.name != null && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }
}

