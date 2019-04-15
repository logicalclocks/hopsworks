package io.hops.hopsworks.common.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GatewayCertsDTO extends CertsDTO {
  
  private String password;
  
  public GatewayCertsDTO() {
  }
  
  public GatewayCertsDTO(String fileExtension, String kStore, String tStore, String password) {
    super(fileExtension, kStore, tStore);
    this.password = password;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
}
