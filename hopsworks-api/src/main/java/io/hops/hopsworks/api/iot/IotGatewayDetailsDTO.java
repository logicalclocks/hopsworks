package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class IotGatewayDetailsDTO extends RestDTO<IotGatewayDetailsDTO> {
  private String name;
  private String domain;
  private Integer port;
  private IotGatewayState state;
  private List<String> blockedDevicesEndpoints;
  private String coapHost;
  private Integer coapPort;
  private String coapsHost;
  private Integer coapsPort;
  private Integer connectedDevices;
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getHostname() {
    return domain;
  }
  
  public void setHostname(String domain) {
    this.domain = domain;
  }
  
  public Integer getPort() {
    return port;
  }
  
  public void setPort(Integer port) {
    this.port = port;
  }
  
  public IotGatewayState getState() {
    return state;
  }
  
  public void setState(IotGatewayState state) {
    this.state = state;
  }
  
  public List<String> getBlockedDevicesEndpoints() {
    return blockedDevicesEndpoints;
  }
  
  public void setBlockedDevicesEndpoints(List<String> blockedDevicesEndpoints) {
    this.blockedDevicesEndpoints = blockedDevicesEndpoints;
  }
  
  public String getCoapHost() {
    return coapHost;
  }
  
  public void setCoapHost(String coapHost) {
    this.coapHost = coapHost;
  }
  
  public Integer getCoapPort() {
    return coapPort;
  }
  
  public void setCoapPort(Integer coapPort) {
    this.coapPort = coapPort;
  }
  
  public String getCoapsHost() {
    return coapsHost;
  }
  
  public void setCoapsHost(String coapsHost) {
    this.coapsHost = coapsHost;
  }
  
  public Integer getCoapsPort() {
    return coapsPort;
  }
  
  public void setCoapsPort(Integer coapsPort) {
    this.coapsPort = coapsPort;
  }
  
  public Integer getConnectedDevices() {
    return connectedDevices;
  }
  
  public void setConnectedDevices(Integer connectedDevices) {
    this.connectedDevices = connectedDevices;
  }
}
