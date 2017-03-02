package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.codec.binary.Base64;

@XmlRootElement
public class EmailJsonDTO {
  private String dest;
  private String subject;
  private String message;
  private String keyStore;
  private String keyStorePwd;
  
  public EmailJsonDTO(){}

  public String getDest() {
    return dest;
  }

  public void setDest(String dest) {
    this.dest = dest;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
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

  public String getKeyStorePwd() {
    return keyStorePwd;
  }

  public void setKeyStorePwd(String keyStorepw) {
    this.keyStorePwd = keyStorepw;
  }
  
  
  
}
