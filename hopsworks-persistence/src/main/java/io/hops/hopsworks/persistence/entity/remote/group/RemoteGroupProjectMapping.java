/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.remote.group;

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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "remote_group_project_mapping",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "RemoteGroupProjectMapping.findAll",
      query = "SELECT l FROM RemoteGroupProjectMapping l")
  ,
    @NamedQuery(name = "RemoteGroupProjectMapping.findById",
      query
      = "SELECT l FROM RemoteGroupProjectMapping l WHERE l.id = :id")
  ,
    @NamedQuery(name = "RemoteGroupProjectMapping.findByGroup",
      query
      = "SELECT l FROM RemoteGroupProjectMapping l WHERE l.remoteGroup = :remoteGroup")
  ,
    @NamedQuery(name = "RemoteGroupProjectMapping.findByGroupAndProject",
      query
        = "SELECT l FROM RemoteGroupProjectMapping l WHERE l.remoteGroup = :remoteGroup AND l.project = :project")
  ,
    @NamedQuery(name = "RemoteGroupProjectMapping.findByRole",
      query
      = "SELECT l FROM RemoteGroupProjectMapping l WHERE l.projectRole = :projectRole")})
public class RemoteGroupProjectMapping implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 256)
  @Column(name = "remote_group")
  private String remoteGroup;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 32)
  @Column(name = "project_role")
  private String projectRole;
  @JoinColumn(name = "project",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public RemoteGroupProjectMapping() {
  }

  public RemoteGroupProjectMapping(String remoteGroup, Project project, String projectRole) {
    this.remoteGroup = remoteGroup;
    this.project = project;
    this.projectRole = projectRole;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getRemoteGroup() {
    return remoteGroup;
  }

  public void setRemoteGroup(String remoteGroup) {
    this.remoteGroup = remoteGroup;
  }

  public String getProjectRole() {
    return projectRole;
  }

  public void setProjectRole(String role) {
    this.projectRole = role;
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
    if (!(object instanceof RemoteGroupProjectMapping)) {
      return false;
    }
    RemoteGroupProjectMapping other = (RemoteGroupProjectMapping) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping[ id=" + id + " ]";
  }
  
}
