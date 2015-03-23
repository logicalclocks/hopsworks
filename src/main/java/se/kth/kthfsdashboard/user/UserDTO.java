/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
public class UserDTO {

  private String username;
  private String email;
  private String name;
  private String password1;
  private String password2;
  private String mobileNum;

  public UserDTO() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword1() {
    return password1;
  }

  public void setPassword1(String password) {
    this.password1 = password;
  }

  public String getPassword2() {
    return password2;
  }

  public void setPassword2(String password) {
    this.password2 = password;
  }

  @Override
  public String toString() {
    return "User [email=" + email + ", name=" + name
            + ", password1=" + password1 + ", password2=" + password2 + "]";
  }

  public String getMobileNum() {
    return mobileNum;
  }

  public void setMobileNum(String mobileNum) {
    this.mobileNum = mobileNum;
  }
}
