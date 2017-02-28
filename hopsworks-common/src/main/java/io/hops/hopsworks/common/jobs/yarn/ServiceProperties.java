package io.hops.hopsworks.common.jobs.yarn;

/**
 *
 * <p>
 */
public class ServiceProperties {

  private KafkaProperties kafka;

  private String keystorePwd;
  private String truststorePwd;
  private Integer projectId;
  private String projectName;
  private String restEndpoint;
  
  public ServiceProperties() {
  }

  public ServiceProperties(String keystorePwd, String truststorePwd,
          Integer projectId, String projectName, String restEndPoint) {
    this.keystorePwd = keystorePwd;
    this.truststorePwd = truststorePwd;
    this.projectId = projectId;
    this.projectName = projectName;
    this.restEndpoint = restEndPoint;
  }

  public String getKeystorePwd() {
    return keystorePwd;
  }

  protected void setKeystorePwd(String keystorePwd) {
    this.keystorePwd = keystorePwd;
  }

  public String getTruststorePwd() {
    return truststorePwd;
  }

  protected void setTruststorePwd(String truststorePwd) {
    this.truststorePwd = truststorePwd;
  }

  public KafkaProperties getKafka() {
    return kafka;
  }

  public void setKafka(KafkaProperties kafka) {
    this.kafka = kafka;
  }

  public void initKafka() {
    kafka = new KafkaProperties();
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public void setRestEndpoint(String restEndPoint) {
    this.restEndpoint = restEndPoint;
  }

}
