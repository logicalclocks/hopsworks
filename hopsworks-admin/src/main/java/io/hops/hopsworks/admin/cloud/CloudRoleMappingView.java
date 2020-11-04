/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.admin.cloud;

import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;

public class CloudRoleMappingView {
  private Integer id;
  private String projectRole;
  private String cloudRole;
  private String projectName;
  private boolean defaultRole;
  
  public CloudRoleMappingView(Integer id, String projectRole, String cloudRole, String projectName,
    boolean defaultRole) {
    this.id = id;
    this.projectRole = projectRole;
    this.cloudRole = cloudRole;
    this.projectName = projectName;
    this.defaultRole = defaultRole;
  }
  
  public CloudRoleMappingView(CloudRoleMapping cloudRoleMapping) {
    this.id = cloudRoleMapping.getId();
    this.projectRole = cloudRoleMapping.getProjectRole();
    this.cloudRole = cloudRoleMapping.getCloudRole();
    this.projectName = cloudRoleMapping.getProjectId().getName();
    this.defaultRole = cloudRoleMapping.isDefaultRole();
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
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public boolean isDefaultRole() {
    return defaultRole;
  }
  
  public void setDefaultRole(boolean defaultRole) {
    this.defaultRole = defaultRole;
  }
  
  @Override
  public String toString() {
    return "CloudRoleMappingView{" +
      "id=" + id +
      ", projectRole='" + projectRole + '\'' +
      ", cloudRole='" + cloudRole + '\'' +
      ", projectName='" + projectName + '\'' +
      ", defaultRole=" + defaultRole +
      '}';
  }
}
