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

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ApiScope {
  JOB(false, true),
  DATASET_VIEW(false, true),
  DATASET_CREATE(false, true),
  DATASET_DELETE(false, true),
  SERVING(false, true),
  FEATURESTORE(false, true),
  PROJECT(false, true),
  ADMIN(true, false),
  KAFKA(false, true),
  ADMINISTER_USERS(true, false),
  ADMINISTER_USERS_REGISTER(true, false),
  MODELREGISTRY(false, true),
  USER(false, true),
  GIT(false, false),
  PYTHON_LIBRARIES(false, false),
  AUTH(true, false);

  private final boolean privileged;
  private final boolean saas;// Hopsworks as a service user scopes

  ApiScope(boolean privileged, boolean saas) {
    this.privileged = privileged;
    this.saas = saas;
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

  public static Set<ApiScope> getServiceUserScopes() {
    return Arrays.stream(ApiScope.values())
      .filter(as -> as.saas)
      .collect(Collectors.toSet());
  }

  private static final Set<ApiScope> AGENT_SCOPES = Sets.newHashSet(AUTH);
  public static Set<ApiScope> getAgentUserScopes() {
    Set<ApiScope> unprivileged = getUnprivileged();
    unprivileged.addAll(AGENT_SCOPES);
    return unprivileged;
  }
}