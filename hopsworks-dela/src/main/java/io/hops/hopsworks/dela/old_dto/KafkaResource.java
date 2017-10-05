package io.hops.hopsworks.dela.old_dto;

public class KafkaResource {

  private String sessionId;
  private String topicName;

  public KafkaResource(String sessionId, String topicName) {
    this.sessionId = sessionId;
    this.topicName = topicName;
  }

  public KafkaResource() {

  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

}
