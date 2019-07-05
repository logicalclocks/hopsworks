package io.hops.hopsworks.api.iot;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IotDevice {
  private String endpoint;
  private String hostname;
  private Integer port;
  private String gatewayName;
  private Integer projectId;
  
  public IotDevice() {
  }
  
  public IotDevice(String endpoint, String hostname, Integer port, String gatewayName, Integer projectId) {
    this.endpoint = endpoint;
    this.hostname = hostname;
    this.port = port;
    this.gatewayName = gatewayName;
    this.projectId = projectId;
  }
  
  public String getEndpoint() {
    return endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
  
  public String getHostname() {
    return hostname;
  }
  
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
  
  public Integer getPort() {
    return port;
  }
  
  public void setPort(Integer port) {
    this.port = port;
  }
  
  public String getGatewayName() {
    return gatewayName;
  }
  
  public void setGatewayName(String gatewayName) {
    this.gatewayName = gatewayName;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  @Override
  public String toString() {
    return "IotDevice{" +
      "endpoint='" + endpoint + '\'' +
      ", hostname='" + hostname + '\'' +
      ", port=" + port +
      ", gatewayName='" + gatewayName + '\'' +
      ", projectId=" + projectId +
      '}';
  }
}
