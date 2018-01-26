package io.hops.hopsworks.api.dela.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DelaClientDTO {

  private String clientType;

  public DelaClientDTO() {
  }

  public DelaClientDTO(String clientType) {
    this.clientType = clientType;
  }

  public String getClientType() {
    return clientType;
  }

  public void setClientType(String clientType) {
    this.clientType = clientType;
  }
}
