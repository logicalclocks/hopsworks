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
package io.hops.hopsworks.persistence.entity.user.security.apiKey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ApiScope {
  JOB(false),
  DATASET_VIEW(false),
  DATASET_CREATE(false),
  DATASET_DELETE(false),
  INFERENCE(false),
  FEATURESTORE(false),
  PROJECT(false),
  ADMIN(true),
  KAFKA(false),
  ADMINISTER_USERS(true),
  ADMINISTER_USERS_REGISTER(true);

  private final boolean privileged;

  ApiScope(boolean privileged) {
    this.privileged = privileged;
  }

  public static ApiScope fromString(String param) {
    return valueOf(param.toUpperCase());
  }

  public static Set<ApiScope> getAll() {
    return new HashSet<>(Arrays.asList(ApiScope.values()));
  }

  public static Set<ApiScope> getPrivileged() {
    return Arrays.stream(ApiScope.values())
            .filter(as -> as.privileged)
            .collect(Collectors.toSet());
  }

  public static Set<ApiScope> getUnprivileged() {
    return Arrays.stream(ApiScope.values())
            .filter(as -> !as.privileged)
            .collect(Collectors.toSet());
  }
}