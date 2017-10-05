package io.hops.hopsworks.dela.dto.common;

import javax.xml.bind.annotation.XmlRootElement;

public class UserDTO {

  @XmlRootElement
  public static class Publish {

    private String firstname;
    private String lastname;
    private String email;

    public Publish() {
    }

    public Publish(String firstname, String lastname, String email) {
      this.firstname = firstname;
      this.lastname = lastname;
      this.email = email;
    }

    public String getFirstname() {
      return firstname;
    }

    public void setFirstname(String firstname) {
      this.firstname = firstname;
    }

    public String getLastname() {
      return lastname;
    }

    public void setLastname(String lastname) {
      this.lastname = lastname;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
  
  @XmlRootElement
  public static class Complete {
    private Integer userId;
    private String firstname;
    private String lastname;
    private String email;
    private String organization;

    public Complete() {
    }

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getFirstname() {
      return firstname;
    }

    public void setFirstname(String firstname) {
      this.firstname = firstname;
    }

    public String getLastname() {
      return lastname;
    }

    public void setLastname(String lastname) {
      this.lastname = lastname;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getOrganization() {
      return organization;
    }

    public void setOrganization(String organization) {
      this.organization = organization;
    }
  }
}
