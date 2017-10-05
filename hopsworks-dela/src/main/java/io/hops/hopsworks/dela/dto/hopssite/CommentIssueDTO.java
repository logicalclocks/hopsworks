package io.hops.hopsworks.dela.dto.hopssite;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CommentIssueDTO {

  private String type;
  private String msg;
  private String userEmail;

  public CommentIssueDTO() {
  }

  public CommentIssueDTO(String type, String msg, String userEmail) {
    this.type = type;
    this.msg = msg;
    this.userEmail = userEmail;
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

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }
}