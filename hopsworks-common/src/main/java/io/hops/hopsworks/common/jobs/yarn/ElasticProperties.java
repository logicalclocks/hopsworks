package io.hops.hopsworks.common.jobs.yarn;

/**
 * POJO that provides Elastic related information to HopsUtil.
 */
public class ElasticProperties {

  private String restEndpoint;

  public ElasticProperties(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public void setRestEndpoint(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }
}
