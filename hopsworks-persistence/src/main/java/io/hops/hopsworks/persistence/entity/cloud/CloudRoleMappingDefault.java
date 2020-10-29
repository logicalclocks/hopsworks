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

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "cloud_role_mapping_default",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "CloudRoleMappingDefault.findAll",
    query = "SELECT c FROM CloudRoleMappingDefault c")
  ,
  @NamedQuery(name = "CloudRoleMappingDefault.findDefaultByProject",
    query = "SELECT c FROM CloudRoleMappingDefault c WHERE c.cloudRoleMapping.projectId = :projectId")
  ,
  @NamedQuery(name = "CloudRoleMappingDefault.findDefaultByProjectAndProjectRole",
    query = "SELECT c FROM CloudRoleMappingDefault c WHERE c.cloudRoleMapping.projectId = :projectId AND " +
      "c.cloudRoleMapping.projectRole = :projectRole")
  ,
  @NamedQuery(name = "CloudRoleMappingDefault.findById",
    query
      = "SELECT c FROM CloudRoleMappingDefault c WHERE c.id = :id")})
public class CloudRoleMappingDefault implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumns({
    @JoinColumn(name = "mapping_id",
        referencedColumnName = "id")
    ,
      @JoinColumn(name = "project_id",
        referencedColumnName = "project_id")
    ,
      @JoinColumn(name = "project_role",
        referencedColumnName = "project_role")})
  @OneToOne(optional = false)
  private CloudRoleMapping cloudRoleMapping;

  public CloudRoleMappingDefault() {
  }

  public CloudRoleMappingDefault(Integer id) {
    this.id = id;
  }

  public CloudRoleMappingDefault(CloudRoleMapping cloudRoleMapping) {
    this.cloudRoleMapping = cloudRoleMapping;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public CloudRoleMapping getCloudRoleMapping() {
    return cloudRoleMapping;
  }

  public void setCloudRoleMapping(CloudRoleMapping cloudRoleMapping) {
    this.cloudRoleMapping = cloudRoleMapping;
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
    if (!(object instanceof CloudRoleMappingDefault)) {
      return false;
    }
    CloudRoleMappingDefault other = (CloudRoleMappingDefault) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.cloud.CloudRoleMappingDefault[ id=" + id + " ]";
  }

}
