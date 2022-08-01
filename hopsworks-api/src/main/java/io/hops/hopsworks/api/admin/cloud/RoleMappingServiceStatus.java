/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
