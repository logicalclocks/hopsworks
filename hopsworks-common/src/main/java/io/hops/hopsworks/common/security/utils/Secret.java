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
package io.hops.hopsworks.common.security.utils;

public final class Secret extends io.hops.hopsworks.api.auth.Secret {

  public Secret(String prefix, String secret, String salt, int prefixMinLength, int secretMinLength,
      int saltMinLength) {
    super(prefix, secret, salt, prefixMinLength, secretMinLength, saltMinLength);
  }

  public Secret(String prefix, String secret, String salt) {
    super(prefix, secret, salt);
  }

  public Secret(String secret, String salt, int secretMinLength, int saltMinLength) {
    super(secret, salt, secretMinLength, saltMinLength);
  }

  public Secret(String secret, String salt) {
    super(secret, salt);
  }
}
