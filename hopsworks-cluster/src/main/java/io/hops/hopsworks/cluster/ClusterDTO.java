package io.hops.hopsworks.cluster;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClusterDTO {
  private String firstName;
  private String email;
  private String chosenPassword;
  private String repeatedPassword;
  private boolean tos;

  public ClusterDTO() {
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getChosenPassword() {
    return chosenPassword;
  }

  public void setChosenPassword(String chosenPassword) {
    this.chosenPassword = chosenPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  public boolean isTos() {
    return tos;
  }

  public void setTos(boolean tos) {
    this.tos = tos;
  }

  @Override
  public String toString() {
    return "ClusterDTO{" + "email=" + email + ", chosenPassword=" + chosenPassword + ", repeatedPassword=" +
        repeatedPassword + ", tos=" + tos + '}';
  }
  
}
