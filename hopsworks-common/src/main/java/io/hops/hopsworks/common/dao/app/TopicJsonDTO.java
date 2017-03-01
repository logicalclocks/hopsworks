package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.codec.binary.Base64;

@XmlRootElement
public class TopicJsonDTO {
  String topicName;
  int version;
  private String keyStore;
  private String keyStorePwd;

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
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

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

}
