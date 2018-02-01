/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.user.cluster;

import javax.xml.bind.annotation.XmlEnumValue;

public enum RegistrationStatusEnum {
  @XmlEnumValue("Registered")
  REGISTERED("Registered"),
  @XmlEnumValue("Registration pending")
  REGISTRATION_PENDING("Registration pending"),
  @XmlEnumValue("Unregistration pending")
  UNREGISTRATION_PENDING("Unregistration pending");

  private final String readable;

  private RegistrationStatusEnum(String readable) {
    this.readable = readable;
  }

  public static RegistrationStatusEnum fromString(String shortName) {
    switch (shortName) {
      case "Registered":
        return RegistrationStatusEnum.REGISTERED;
      case "Registration pending":
        return RegistrationStatusEnum.REGISTRATION_PENDING;
      case "Unregistration pending":
        return RegistrationStatusEnum.UNREGISTRATION_PENDING;
      default:
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
  }

  @Override
  public String toString() {
    return this.readable;
  }

}
