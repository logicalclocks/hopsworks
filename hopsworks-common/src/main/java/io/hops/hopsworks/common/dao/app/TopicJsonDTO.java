package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicJsonDTO extends KeystoreDTO {

  String topicName;
  int version;

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

}
