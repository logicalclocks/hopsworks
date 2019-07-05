package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IotDeviceDTO extends RestDTO<IotDeviceDTO> {
  private String endpoint;
  private String hostname;
  private int port;
  private String gatewayName;
  private Integer projectId;
  
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
  
  public int getPort() {
    return port;
  }
  
  public void setPort(int port) {
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
    return "IotDeviceDTO{" +
      "endpoint='" + endpoint + '\'' +
      ", hostname='" + hostname + '\'' +
      ", port=" + port +
      ", gatewayName='" + gatewayName + '\'' +
      ", projectId=" + projectId +
      '}';
  }
}
