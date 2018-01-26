package io.hops.hopsworks.common.dao.device;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class ProjectDevicesSettingsDTO implements Serializable {

  private Integer enabled;

  private Integer jwtTokenDurationInHours;

  public ProjectDevicesSettingsDTO(){}

  public ProjectDevicesSettingsDTO(Integer enabled, Integer jwtTokenDurationInHours) {
    this.enabled = enabled;
    this.jwtTokenDurationInHours = jwtTokenDurationInHours;
  }

  public Integer getEnabled() {
    return enabled;
  }

  public void setEnabled(Integer enabled) {
    this.enabled = enabled;
  }

  public Integer getJwtTokenDurationInHours() {
    return jwtTokenDurationInHours;
  }

  public void setJwtTokenDurationInHours(Integer jwtTokenDurationInHours) {
    this.jwtTokenDurationInHours = jwtTokenDurationInHours;
  }
}
