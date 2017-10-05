package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class KafkaEndpoint {

  private String brokerEndpoint;
  private String restEndpoint;
  private String domain;
  private String projectId;
  private String keyStore;
  private String trustStore;

  public KafkaEndpoint() {
  }

  public KafkaEndpoint(String brokerEndpoint, String restEndpoint, String domain,
          String projectId, String keyStore, String trustStore) {
    this.brokerEndpoint = brokerEndpoint;
    this.restEndpoint = restEndpoint;
    this.domain = domain;
    this.projectId = projectId;
    this.keyStore = keyStore;
    this.trustStore = trustStore;
  }

  public String getBrokerEndpoint() {
    return brokerEndpoint;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public String getDomain() {
    return domain;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getKeyStore() {
    return keyStore;
  }

  public String getTrustStore() {
    return trustStore;
  }

  public void setBrokerEndpoint(String brokerEndpoint) {
    this.brokerEndpoint = brokerEndpoint;
  }

  public void setRestEndpoint(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }

  public void setTrustStore(String trustStore) {
    this.trustStore = trustStore;
  }

}
