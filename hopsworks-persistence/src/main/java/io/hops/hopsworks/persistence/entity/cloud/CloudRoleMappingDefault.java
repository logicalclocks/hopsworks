/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
