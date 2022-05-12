/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RoleMappingServiceStatus {
  private boolean enabled;
  
  public RoleMappingServiceStatus() {
  }
  
  public RoleMappingServiceStatus(boolean enabled) {
    this.enabled = enabled;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  @Override
  public String toString() {
    return "RoleMappingServiceStatus{" +
      "enabled=" + enabled +
      '}';
  }
}
