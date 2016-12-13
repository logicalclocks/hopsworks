package se.kth.bbc.jobs.yarn;

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
