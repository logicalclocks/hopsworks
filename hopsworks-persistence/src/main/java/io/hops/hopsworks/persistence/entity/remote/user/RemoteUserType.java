/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

@XmlType(name = "remoteUserType")
@XmlEnum
public enum RemoteUserType {
  @XmlEnumValue("OAUTH2")
  OAUTH2(0),
  @XmlEnumValue("LDAP")
  LDAP(1),
  @XmlEnumValue("KRB")
  KRB(2);

  private final int value;

  private RemoteUserType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static RemoteUserType fromValue(int v) {
    for (RemoteUserType c : RemoteUserType.values()) {
      if (c.value == v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
