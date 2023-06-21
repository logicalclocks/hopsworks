/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;

public class MappingDTO extends RestDTO<MappingDTO> {
  private Integer id;
  private String remoteGroup;
  private String projectRole;
  private String projectName;
  private Integer projectId;
  
  public MappingDTO() {
  }
  
  public MappingDTO(Integer id, String remoteGroup, String projectRole, String projectName, Integer projectId) {
    this.id = id;
    this.remoteGroup = remoteGroup;
    this.projectRole = projectRole;
    this.projectName = projectName;
    this.projectId = projectId;
  }
  
  public MappingDTO(String remoteGroup, String projectRole, String projectName, Integer projectId) {
    this.remoteGroup = remoteGroup;
    this.projectRole = projectRole;
    this.projectName = projectName;
    this.projectId = projectId;
  }
  
  public MappingDTO(RemoteGroupProjectMapping remoteGroupProjectMapping) {
    this.id = remoteGroupProjectMapping.getId();
    this.remoteGroup = remoteGroupProjectMapping.getRemoteGroup();
    this.projectRole = remoteGroupProjectMapping.getProjectRole();
    this.projectName = remoteGroupProjectMapping.getProject().getName();
    this.projectId = remoteGroupProjectMapping.getProject().getId();
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
    return "MappingDTO{" +
      "id=" + id +
      ", remoteGroup='" + remoteGroup + '\'' +
      ", projectRole='" + projectRole + '\'' +
      ", projectName='" + projectName + '\'' +
      ", projectId=" + projectId +
      '}';
  }
}
