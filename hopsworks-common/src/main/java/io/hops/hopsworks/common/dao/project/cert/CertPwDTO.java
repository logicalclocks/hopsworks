package io.hops.hopsworks.common.dao.project.cert;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * 
 */
@XmlRootElement
public class CertPwDTO {

  private String keyPw;
  private String trustPw;

  public CertPwDTO() {
  }

  public String getKeyPw() {
    return keyPw;
  }

  public void setKeyPw(String keyPw) {
    this.keyPw = keyPw;
  }

  public String getTrustPw() {
    return trustPw;
  }

  public void setTrustPw(String trustPw) {
    this.trustPw = trustPw;
  }

}
