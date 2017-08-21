package io.hops.hopsworks.common.dao.certificates;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserCertsDTO {

  private byte[] userKey;
  private byte[] userCert;
  private String userKeyPwd;
  private String userCertPwd;

  public UserCertsDTO() {
  }

  public UserCertsDTO(byte[] userKey, byte[] userCert, String userKeyPwd, String userCertPwd) {
    this.userKey = userKey;
    this.userCert = userCert;
    this.userKeyPwd = userKeyPwd;
    this.userCertPwd = userCertPwd;
  }

  public byte[] getUserKey() {
    return userKey;
  }

  public void setUserKey(byte[] userKey) {
    this.userKey = userKey;
  }

  public byte[] getUserCert() {
    return userCert;
  }

  public void setUserCert(byte[] userCert) {
    this.userCert = userCert;
  }

  public String getUserKeyPwd() {
    return userKeyPwd;
  }

  public void setUserKeyPwd(String userKeyPwd) {
    this.userKeyPwd = userKeyPwd;
  }

  public String getUserCertPwd() {
    return userCertPwd;
  }

  public void setUserCertPwd(String userCertPwd) {
    this.userCertPwd = userCertPwd;
  }

}
