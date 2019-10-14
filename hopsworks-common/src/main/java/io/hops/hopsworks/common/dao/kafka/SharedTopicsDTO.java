package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SharedTopicsDTO extends RestDTO<SharedTopicsDTO> {
  private Integer projectId;
  private SharedTopicsPK sharedTopicsPK;
  
  public SharedTopicsDTO() {
  }
  
  public SharedTopicsDTO(Integer projectId, SharedTopicsPK sharedTopicsPK) {
    this.projectId = projectId;
    this.sharedTopicsPK = sharedTopicsPK;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public SharedTopicsPK getSharedTopicsPK() {
    return sharedTopicsPK;
  }
  
  public void setSharedTopicsPK(SharedTopicsPK sharedTopicsPK) {
    this.sharedTopicsPK = sharedTopicsPK;
  }
}
