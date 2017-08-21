package io.hops.hopsworks.common.dao.project.cert;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * 
 */
@XmlRootElement
public class CertPwReqDTO {

  private String keyStore;

  public CertPwReqDTO() {
  }

  public byte[] getKeyStoreBytes() {
    return Base64.decodeBase64(keyStore);
  }

  public String getKeyStore() {
    return keyStore;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }
}
