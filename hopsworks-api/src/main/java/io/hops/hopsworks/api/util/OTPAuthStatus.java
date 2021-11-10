/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.common.util.Settings;

public enum OTPAuthStatus {
  DISABLED("DISABLED"),
  MANDATORY("MANDATORY"),
  OPTIONAL("OPTIONAL");
  
  private final String name;
  
  
  OTPAuthStatus(String name) {
    this.name = name;
  }
  
  public static OTPAuthStatus fromTwoFactorMode(String twoFactorMode) {
    if (twoFactorMode.equals(Settings.TwoFactorMode.MANDATORY.getName())) {
      return OTPAuthStatus.MANDATORY;
    } else if (twoFactorMode.equals(Settings.TwoFactorMode.OPTIONAL.getName())) {
      return OTPAuthStatus.OPTIONAL;
    } else {
      return OTPAuthStatus.DISABLED;
    }
  }
}
