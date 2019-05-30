package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.dao.iot.IotGateways;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class IotGatewayDetails {
  private IotGateways iotGateway;
  private List<String> blockedDevicesEndpoints;
  private String coapHost;
  private Integer coapPort;
  private String coapsHost;
  private Integer coapsPort;
  private Integer connectedDevices;
  
  public IotGatewayDetails() {
  }
  
  public IotGatewayDetails(List<String> blockedDevicesEndpoints, String coapHost, Integer coapPort,
    String coapsHost, Integer coapsPort, Integer connectedDevices, IotGateways iotGateway) {
    this.blockedDevicesEndpoints = blockedDevicesEndpoints;
    this.coapHost = coapHost;
    this.coapPort = coapPort;
    this.coapsHost = coapsHost;
    this.coapsPort = coapsPort;
    this.connectedDevices = connectedDevices;
    this.iotGateway = iotGateway;
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
  
  public IotGateways getIotGateway() {
    return iotGateway;
  }
  
  public void setIotGateway(IotGateways iotGateway) {
    this.iotGateway = iotGateway;
  }
  
  @Override
  public String toString() {
    return "IotGatewayDetails{" +
      "iotGateway=" + iotGateway +
      ", blockedDevicesEndpoints=" + blockedDevicesEndpoints +
      ", coapHost='" + coapHost + '\'' +
      ", coapPort=" + coapPort +
      ", coapsHost='" + coapsHost + '\'' +
      ", coapsPort=" + coapsPort +
      ", connectedDevices=" + connectedDevices +
      '}';
  }
}
