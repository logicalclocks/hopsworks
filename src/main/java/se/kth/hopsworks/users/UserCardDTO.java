package se.kth.hopsworks.users;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.hopsworks.user.model.Users;

/**
 * Holds only Name and email of the user
 * <p>
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@XmlRootElement
public class UserCardDTO implements Serializable {

  private String firstname;
  private String lastname;
  private String email;

  public UserCardDTO(Users user) {
    this.firstname = user.getFname();
    this.lastname = user.getLname();
    this.email = user.getEmail();
  }

  public UserCardDTO(String firstname, String lastname, String email) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
  }

  public UserCardDTO() {
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
