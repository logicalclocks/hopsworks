/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.api.auth.Configuration;
import io.hops.hopsworks.api.auth.UserStatusValidator;
import io.hops.hopsworks.api.auth.UserUtilities;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.auth.key.ApiKeyUtilities;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTLogLevel;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@Provider
@ApiKeyRequired
@Priority(Priorities.AUTHENTICATION - 1)
public class ApiKeyFilter extends io.hops.hopsworks.api.auth.key.ApiKeyFilter {
  @EJB
  private UserStatusValidator userStatusValidator;
  @EJB
  private ApiKeyUtilities apiKeyUtilities;
  @EJB
  private UserUtilities userUtilities;
  @EJB
  private Configuration conf;
  @Context
  private ResourceInfo resourceInfo;

  protected void validateUserStatus(Users user) throws UserException {
    userStatusValidator.checkStatus(user.getStatus());
  }

  protected ApiKey getApiKey(String key) throws ApiKeyException {
    return apiKeyUtilities.getApiKey(key);
  }

  protected Set<ApiScope> getApiScopes(ApiKey key) {
    return apiKeyUtilities.getScopes(key);
  }

  protected List<String> getUserRoles(Users user) {
    return userUtilities.getUserRoles(user);
  }

  protected RESTLogLevel getRestLogLevel() {
    return conf.getLogLevel(Configuration.AuthConfigurationKeys.HOPSWORKS_REST_LOG_LEVEL);
  }

  protected Class<?> getResourceClass() {
    return resourceInfo.getResourceClass();
  }

  protected Method getResourceMethod() {
    return resourceInfo.getResourceMethod();
  }
}
