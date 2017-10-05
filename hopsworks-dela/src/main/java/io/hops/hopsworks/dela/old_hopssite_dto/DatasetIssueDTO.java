package io.hops.hopsworks.dela.old_hopssite_dto;

import io.hops.hopsworks.dela.dto.common.UserDTO;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DatasetIssueDTO {

  private String type;
  private String msg;
  private UserDTO.Complete user;
  private String publicDSId;

  public DatasetIssueDTO() {
  }

  public DatasetIssueDTO(String publicDSId, UserDTO.Complete user, String type, String msg) {
    this.type = type;
    this.msg = msg;
    this.user = user;
    this.publicDSId = publicDSId;
  }
  
  public UserDTO.Complete getUser() {
    return user;
  }

  public void setUser(UserDTO.Complete user) {
    this.user = user;
  }

  public String getPublicDSId() {
    return publicDSId;
  }

  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

}