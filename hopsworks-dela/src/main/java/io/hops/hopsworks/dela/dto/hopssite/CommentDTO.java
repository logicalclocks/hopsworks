package io.hops.hopsworks.dela.dto.hopssite;

import io.hops.hopsworks.dela.dto.common.UserDTO;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

public class CommentDTO {

  @XmlRootElement
  public static class Publish {

    private String content;
    private String userEmail;

    public Publish() {
    }

    public Publish(String userEmail, String content) {
      this.content = content;
      this.userEmail = userEmail;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public String getUserEmail() {
      return userEmail;
    }

    public void setUserEmail(String userEmail) {
      this.userEmail = userEmail;
    }
  }

  @XmlRootElement
  public static class RetrieveComment {

    private Integer id;
    private String content;
    private UserDTO.Complete user;
    private Date datePublished;

    public RetrieveComment() {
    }

    public RetrieveComment(Integer id, String content, UserDTO.Complete user, Date datePublished) {
      this.id = id;
      this.content = content;
      this.user = user;
      this.datePublished = datePublished;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public UserDTO.Complete getUser() {
      return user;
    }

    public void setUser(UserDTO.Complete user) {
      this.user = user;
    }

    public Date getDatePublished() {
      return datePublished;
    }

    public void setDatePublished(Date datePublished) {
      this.datePublished = datePublished;
    }
  }
}
