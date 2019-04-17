package io.hops.hopsworks.common.dao.iot;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public class IotGatewayConfiguration {
  @XmlElement
  private Integer gatewayId;
  @XmlElement
  private String hostname;
  @XmlElement
  private Integer port;
  
  public IotGatewayConfiguration() {
  }
  
  public IotGatewayConfiguration(Integer gatewayId, String hostname, Integer port) {
    this.gatewayId = gatewayId;
    this.hostname = hostname;
    this.port = port;
  }
  
  public Integer getGatewayId() {
    return gatewayId;
  }
  
  public void setGatewayId(Integer gatewayId) {
    this.gatewayId = gatewayId;
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
}
