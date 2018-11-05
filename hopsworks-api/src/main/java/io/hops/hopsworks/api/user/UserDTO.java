package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserDTO extends RestDTO<Users, UserDTO> {
  
  private String firstname;
  private String lastname;
  private String email;
  
  public UserDTO() {
  }
  
  public UserDTO(Users user) {
    this.firstname = user.getFname();
    this.lastname = user.getLname();
    this.email = user.getEmail();
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
