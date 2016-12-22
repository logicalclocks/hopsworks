package se.kth.bbc.jobs.yarn;

import java.io.File;

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

  public void setConsumerGroups(String consumerGroups) {
    this.consumerGroups = consumerGroups;
  }

  /**
   * Append project name to every consumer group.
   *
   * @param projectName
   * @param consumerGroups
   */
  public void setProjectConsumerGroups(String projectName, String consumerGroups) {
    String[] groups = consumerGroups.split(File.pathSeparator);
    //Append projectName for every group so actions on them can be properly authenticated
    for (String group : groups) {
      this.consumerGroups += projectName + "-" + group + File.pathSeparator;
    }
    this.consumerGroups = this.consumerGroups.substring(this.consumerGroups.
            lastIndexOf(File.pathSeparator));
  }

  public String getTopics() {
    return topics;
  }

  public void setTopics(String topics) {
    this.topics = topics;
  }

  @Override
  public String toString() {
    return "KafkaProperties{" + "address=" + brokerAddresses
            + ", restEndpoint=" + restEndpoint + ", consumerGroups="
            + consumerGroups + ", topics=" + topics + '}';
  }

}
