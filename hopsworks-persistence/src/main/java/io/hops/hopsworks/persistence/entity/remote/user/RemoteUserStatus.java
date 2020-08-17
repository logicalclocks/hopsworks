/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.remote.user;


import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "remoteUserStatus")
@XmlEnum
public enum RemoteUserStatus {
  @XmlEnumValue("Active")
  ACTIVE(0, "Active"),
  @XmlEnumValue("Deleted")
  DELETED(1, "Deleted from remote"),
  @XmlEnumValue("Deactivated")
  DEACTIVATED(2, "Deleted from remote and removed from all projects");
  
  private final int value;
  private final String description;
  
  private RemoteUserStatus(int value, String description) {
    this.value = value;
    this.description = description;
  }
  
  public int getValue() {
    return value;
  }
  
  public String getDescription() {
    return description;
  }
  
  public static RemoteUserStatus fromValue(int v) {
    for (RemoteUserStatus c : RemoteUserStatus.values()) {
      if (c.value == v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
