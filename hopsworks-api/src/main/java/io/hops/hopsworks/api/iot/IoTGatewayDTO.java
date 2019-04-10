package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.iot.GatewayState;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IoTGatewayDTO extends RestDTO<IoTGatewayDTO> {
  private Integer id;
  private String ipAddress;
  private Integer port;
  private GatewayState state;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }
  
  public GatewayState getState() {
    return state;
  }
  
  public void setState(GatewayState state) {
    this.state = state;
  }
}
