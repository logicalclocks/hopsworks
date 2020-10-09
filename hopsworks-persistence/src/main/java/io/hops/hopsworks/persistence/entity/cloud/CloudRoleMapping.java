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
package io.hops.hopsworks.persistence.entity.cloud;

import io.hops.hopsworks.persistence.entity.project.Project;
import java.io.Serializable;
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

@Entity
@Table(name = "cloud_role_mapping",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "CloudRoleMapping.findAll",
      query = "SELECT c FROM CloudRoleMapping c")
  ,
    @NamedQuery(name = "CloudRoleMapping.findById",
      query = "SELECT c FROM CloudRoleMapping c WHERE c.id = :id")
  ,
  @NamedQuery(name = "CloudRoleMapping.findByIdAndProject",
    query = "SELECT c FROM CloudRoleMapping c WHERE c.id = :id AND c.projectId = :projectId")
  ,
    @NamedQuery(name = "CloudRoleMapping.findByProjectRole",
      query
      = "SELECT c FROM CloudRoleMapping c WHERE c.projectRole = :projectRole")
  ,
    @NamedQuery(name = "CloudRoleMapping.findByProjectAndCloudRole",
      query
      = "SELECT c FROM CloudRoleMapping c WHERE c.projectId = :projectId AND c.cloudRole = :cloudRole")
  ,
    @NamedQuery(name = "CloudRoleMapping.findByCloudRole",
      query
      = "SELECT c FROM CloudRoleMapping c WHERE c.cloudRole = :cloudRole")})
public class CloudRoleMapping implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 32)
  @Column(name = "project_role")
  private String projectRole;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 2048)
  @Column(name = "cloud_role")
  private String cloudRole;
  @JoinColumn(name = "project_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;

  public CloudRoleMapping() {
  }

  public CloudRoleMapping(Integer id) {
    this.id = id;
  }

  public CloudRoleMapping(Project projectId, String projectRole, String cloudRole) {
    this.projectId = projectId;
    this.projectRole = projectRole;
    this.cloudRole = cloudRole;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getProjectRole() {
    return projectRole;
  }

  public void setProjectRole(String projectRole) {
    this.projectRole = projectRole;
  }

  public String getCloudRole() {
    return cloudRole;
  }

  public void setCloudRole(String cloudRole) {
    this.cloudRole = cloudRole;
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
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof CloudRoleMapping)) {
      return false;
    }
    CloudRoleMapping other = (CloudRoleMapping) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping[ id=" + id + " ]";
  }

}
