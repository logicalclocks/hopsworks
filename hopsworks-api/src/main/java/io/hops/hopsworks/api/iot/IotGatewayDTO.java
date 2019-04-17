package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IotGatewayDTO extends RestDTO<IotGatewayDTO> {
  private Integer id;
  private String hostname;
  private Integer port;
  private IotGatewayState state;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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
  
  public IotGatewayState getState() {
    return state;
  }
  
  public void setState(IotGatewayState state) {
    this.state = state;
  }
}
