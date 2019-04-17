package io.hops.hopsworks.api.iot;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IotDevice {
  private String endpoint;
  private String hostname;
  private Integer port;
  private Integer gatewayId;
  
  public IotDevice() {
  }
  
  public IotDevice(String endpoint, String hostname, Integer port, Integer gatewayId) {
    this.endpoint = endpoint;
    this.hostname = hostname;
    this.port = port;
    this.gatewayId = gatewayId;
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
  
  public Integer getGatewayId() {
    return gatewayId;
  }
  
  public void setGatewayId(Integer gatewayId) {
    this.gatewayId = gatewayId;
  }
  
  @Override
  public String toString() {
    return "IotDevice(" + endpoint + "," +
      hostname + "," +
      port + "," +
      gatewayId + ")";
  }
}
