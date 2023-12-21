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
package io.hops.hopsworks.api.auth.key;

import io.hops.hopsworks.api.auth.Secret;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKeyScope;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ApiKeyUtilities {
  @EJB
  private ApiKeyFacade apiKeyFacade;

  public ApiKey getApiKey(String key) throws ApiKeyException {
    String[] parts = key.split(Secret.KEY_ID_SEPARATOR_REGEX);
    if (parts.length < 2) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_INVALID, Level.FINE);
    }
    ApiKey apiKey = apiKeyFacade.findByPrefix(parts[0]);
    if (apiKey == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND_IN_DATABASE, Level.FINE);
    }
    //___MinLength can be set to 0 b/c no validation is needed if the key was in db
    Secret secret = new Secret(parts[0], parts[1], apiKey.getSalt());
    if (!secret.getSha256HexDigest().equals(apiKey.getSecret())) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_INVALID, Level.FINE);
    }
    return apiKey;
  }

  public Set<ApiScope> getScopes(ApiKey apiKey) {
    Set<ApiScope> scopes = new HashSet<>();
    for (ApiKeyScope scope : apiKey.getApiKeyScopeCollection()) {
      scopes.add(scope.getScope());
    }
    return scopes;
  }
}
