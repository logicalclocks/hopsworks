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

package io.hops.hopsworks.common.user.security.apiKey;

import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKeyScope;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.enterprise.inject.Instance;
import java.util.Collection;
import java.util.logging.Level;

public interface ApiKeyHandler {
  void create(ApiKey apiKey) throws Exception;
  void delete(ApiKey apiKey) throws Exception;
  
  boolean match(Collection<ApiKeyScope> scopes);
  
  String getClassName();
  
  static void runApiKeyCreateHandlers(Instance<ApiKeyHandler> apiKeyHandlers, ApiKey apiKey) throws ApiKeyException {
    runApiKeyCreateHandlers(apiKeyHandlers, apiKey, apiKey.getApiKeyScopeCollection());
  }
  static void runApiKeyCreateHandlers(Instance<ApiKeyHandler> apiKeyHandlers, ApiKey apiKey,
      Collection<ApiKeyScope> scopes) throws ApiKeyException {
    for (ApiKeyHandler apiKeyHandler : apiKeyHandlers) {
      try {
        if (apiKeyHandler.match(scopes)) {
          apiKeyHandler.create(apiKey);
        }
      } catch (Exception e) {
        throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_HANDLER_CREATE_ERROR, Level.SEVERE,
          e.getMessage(), "apikey: " + apiKey.getName() + ", handler: " + apiKeyHandler.getClassName(), e);
      }
    }
  }
  
  static void runApiKeyDeleteHandlers(Instance<ApiKeyHandler> apiKeyHandlers, ApiKey apiKey) throws ApiKeyException {
    runApiKeyDeleteHandlers(apiKeyHandlers, apiKey, apiKey.getApiKeyScopeCollection());
  }
  static void runApiKeyDeleteHandlers(Instance<ApiKeyHandler> apiKeyHandlers, ApiKey apiKey,
      Collection<ApiKeyScope> scopes) throws ApiKeyException {
    for (ApiKeyHandler apiKeyHandler : apiKeyHandlers) {
      try {
        if (apiKeyHandler.match(scopes)) {
          apiKeyHandler.delete(apiKey);
        }
      } catch (Exception e) {
        throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_HANDLER_DELETE_ERROR, Level.SEVERE,
          e.getMessage(), "apikey: " + apiKey.getName() + ", handler: " + apiKeyHandler.getClassName(), e);
      }
    }
  }
}
