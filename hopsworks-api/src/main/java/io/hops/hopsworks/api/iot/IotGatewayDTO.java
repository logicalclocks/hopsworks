package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IotGatewayDTO extends RestDTO<IotGatewayDTO> {
  private String name;
  private String domain;
  private Integer port;
  private IotGatewayState state;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
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
}
