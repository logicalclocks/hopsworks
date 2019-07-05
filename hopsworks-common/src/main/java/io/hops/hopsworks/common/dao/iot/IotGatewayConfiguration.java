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
  private String name;
  @XmlElement
  private String domain;
  @XmlElement
  private Integer port;
  
  public IotGatewayConfiguration() {
  }
  
  public IotGatewayConfiguration(String name, String domain, Integer port) {
    this.name = name;
    this.domain = domain;
    this.port = port;
  }
  
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
}
