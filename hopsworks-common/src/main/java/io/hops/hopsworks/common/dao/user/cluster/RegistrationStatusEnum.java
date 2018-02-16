/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
