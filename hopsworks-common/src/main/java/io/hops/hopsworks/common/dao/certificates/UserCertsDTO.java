package io.hops.hopsworks.common.dao.certificates;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserCertsDTO {

  private byte[] userKey;
  private byte[] userCert;

  public UserCertsDTO() {
  }

  public UserCertsDTO(byte[] userKey, byte[] userCert) {
    this.userKey = userKey;
    this.userCert = userCert;
  }

  public byte[] getUserCert() {
    return userCert;
  }

  public void setUserCert(byte[] userCert) {
    this.userCert = userCert;
  }

  public byte[] getUserKey() {
    return userKey;
  }

  public void setUserKey(byte[] userKey) {
    this.userKey = userKey;
  }
}
