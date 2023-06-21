/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import java.util.Set;

public class BulkMappingDTO {
  
  private Set<String> remoteGroup;
  private String projectRole;
  private String projectName;
  private Integer projectId;
  
  public BulkMappingDTO() {
  }
  
  public Set<String> getRemoteGroup() {
    return remoteGroup;
  }
  
  public void setRemoteGroup(Set<String> remoteGroup) {
    this.remoteGroup = remoteGroup;
  }
  
  public String getProjectRole() {
    return projectRole;
  }
  
  public void setProjectRole(String projectRole) {
    this.projectRole = projectRole;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  @Override
  public String toString() {
    return "BulkMappingDTO{" +
      "remoteGroup=" + remoteGroup +
      ", projectRole='" + projectRole + '\'' +
      ", projectName='" + projectName + '\'' +
      ", projectId=" + projectId +
      '}';
  }
}
