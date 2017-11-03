package io.hops.hopsworks.cluster;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClusterDTO {
  private String email;
  private String chosenPassword;
  private String repeatedPassword;
  private String commonName;
  private String organizationName;
  private String organizationalUnitName;
  private boolean tos;

  public ClusterDTO() {
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

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getOrganizationalUnitName() {
    return organizationalUnitName;
  }

  public void setOrganizationalUnitName(String organizationalUnitName) {
    this.organizationalUnitName = organizationalUnitName;
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
        repeatedPassword + ", commonName=" + commonName + ", organizationName=" + organizationName +
        ", organizationalUnitName=" + organizationalUnitName + ", tos=" + tos + '}';
  }
  
}
