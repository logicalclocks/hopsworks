package se.kth.bbc.jobs.yarn;

import java.io.File;
import se.kth.bbc.jobs.model.configuration.ConsumerGroupDTO;
import se.kth.bbc.jobs.model.configuration.KafkaTopicDTO;
import se.kth.hopsworks.util.Settings;

/**
 * POJO that provides Kafka related information to HopsUtil.
 */
public class KafkaProperties extends ServiceProperties {

  private String brokerAddresses;
  private String sessionId;
  private String restEndpoint;
  //Comma-separated list of consumer groups
  private String consumerGroups;
  //Comma-separated list of topics;
  private String topics;

  public KafkaProperties() {
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getBrokerAddresses() {
    return brokerAddresses;
  }

  public void setBrokerAddresses(String brokerAddresses) {
    this.brokerAddresses = brokerAddresses;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public void setRestEndpoint(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }

  public String getConsumerGroups() {
    return consumerGroups;
  }

  /**
   * Append project name to every consumer group. Sets the default consumer
   * group.
   *
   * @param projectName
   * @param consumerGroups
   */
  public void setProjectConsumerGroups(String projectName,
          ConsumerGroupDTO[] consumerGroups) {
    this.consumerGroups = "";
    StringBuilder sb = new StringBuilder();
    sb.append(projectName).append("__").append(
            Settings.KAFKA_DEFAULT_CONSUMER_GROUP).append(File.pathSeparator);
    if (consumerGroups != null) {
      for (ConsumerGroupDTO consumerGroup : consumerGroups) {
        if (!consumerGroup.getName().equals(
                Settings.KAFKA_DEFAULT_CONSUMER_GROUP)) {
          sb.append(projectName).append("__").append(consumerGroup.getName()).
                  append(File.pathSeparator);
        }
      }
    }
    this.consumerGroups += sb.substring(0, sb.lastIndexOf(File.pathSeparator));
  }

  public String getTopics() {
    return topics;
  }

  public void setTopics(KafkaTopicDTO[] topics) {
    this.topics = "";
    StringBuilder sb = new StringBuilder();
    for (KafkaTopicDTO topic : topics) {
      sb.append(topic.getName()).append(File.pathSeparator);
    }
    this.topics = sb.substring(0, sb.lastIndexOf(File.pathSeparator));
  }

  @Override
  public String toString() {
    return "KafkaProperties{" + "brokerAddresses=" + brokerAddresses
            + ", sessionId=" + sessionId + ", restEndpoint=" + restEndpoint
            + ", consumerGroups=" + consumerGroups + ", topics=" + topics + '}';
  }

}
