/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CloudRoleMappingDTO extends RestDTO<CloudRoleMappingDTO> {
  private Integer id;
  private String projectRole;
  private String cloudRole;
  private Integer projectId;
  private String projectName;
  private boolean defaultRole;
  
  public CloudRoleMappingDTO() {
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
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
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
}
