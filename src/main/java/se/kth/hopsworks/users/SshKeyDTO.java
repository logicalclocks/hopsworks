package se.kth.hopsworks.users;

import se.kth.hopsworks.user.model.SshKeys;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class SshKeyDTO {

  private String name;
  private String publicKey;

  public SshKeyDTO() {
  }

  public SshKeyDTO(SshKeys key) {
    this.name = key.getSshKeysPK().getName();
    this.publicKey = key.getPublicKey();
  }

  public String getName() {
    return name;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public String toString() {
    return "SshkeyDTO{name=" + name + "publicKey=" + publicKey + '}';
  }

}
