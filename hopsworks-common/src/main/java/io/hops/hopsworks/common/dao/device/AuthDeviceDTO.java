package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class AuthDeviceDTO implements Serializable{

  private String deviceUuid;

  private String password;

  private String alias;

  public AuthDeviceDTO() {
  }

  public AuthDeviceDTO(String deviceUuid, String password) {
    this.deviceUuid = deviceUuid;
    this.password = password;
  }

  public AuthDeviceDTO(String deviceUuid, String password, String alias) {
    this.deviceUuid = deviceUuid;
    this.password = password;
    this.alias = alias;
  }

  public String getDeviceUuid() {
    return deviceUuid;
  }

  public void setDeviceUuid(String deviceUuid) {
    this.deviceUuid = deviceUuid;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

}
