package io.hops.hopsworks.common.dao.dataset;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RequestDTO {

  private Integer inodeId;
  private Integer projectId;
  private String messageContent;

  public RequestDTO() {
  }

  public RequestDTO(Integer inodeId, Integer projectId, String message) {
    this.inodeId = inodeId;
    this.projectId = projectId;
    this.messageContent = message;
  }

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getMessageContent() {
    return messageContent;
  }

  public void setMessageContent(String message) {
    this.messageContent = message;
  }

}
