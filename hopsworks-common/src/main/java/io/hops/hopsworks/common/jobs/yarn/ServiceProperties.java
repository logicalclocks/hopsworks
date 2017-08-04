package io.hops.hopsworks.common.jobs.yarn;

/**
 *
 * <p>
 */
public class ServiceProperties {

  private KafkaProperties kafka;
  private AnacondaProperties anaconda;
  private ElasticProperties elastic;

  private Integer projectId;
  private String projectName;
  private String restEndpoint;
  private String jobName;

  public ServiceProperties() {
  }

  public ServiceProperties(Integer projectId, String projectName, String restEndPoint, String jobName,
      ElasticProperties elastic) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.restEndpoint = restEndPoint;
    this.jobName = jobName;
    this.elastic = elastic;
  }

  public KafkaProperties getKafka() {
    return kafka;
  }

  public boolean isKafkaEnabled() {
    return kafka != null;
  }

  public void setKafka(KafkaProperties kafka) {
    this.kafka = kafka;
  }

  public void initKafka() {
    kafka = new KafkaProperties();
  }

  public boolean isAnacondaEnabled() {
    return anaconda != null;
  }

  public AnacondaProperties getAnaconda() {
    return anaconda;
  }

  public void setAnaconda(AnacondaProperties anaconda) {
    this.anaconda = anaconda;
  }

  public void initAnaconda(String envPath) {
    anaconda = new AnacondaProperties(envPath);
  }

  public ElasticProperties getElastic() {
    return elastic;
  }

  public void setElastic(ElasticProperties elastic) {
    this.elastic = elastic;
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

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

}
