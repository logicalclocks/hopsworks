package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IoTDeviceDTO extends RestDTO<IoTDeviceDTO> {
  private String endpoint;
  private String hostname;
  private int port;
  private int gatewayId;
  
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
  
  public int getGatewayId() {
    return gatewayId;
  }
  
  public void setGatewayId(int gatewayId) {
    this.gatewayId = gatewayId;
  }
  
  @Override
  public String toString() {
    return "IoTDeviceDTO(" + endpoint + "," +
      hostname + "," +
      port + "," +
      gatewayId + ")";
  }
}
