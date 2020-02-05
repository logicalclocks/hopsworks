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
package io.hops.hopsworks.common.user.security.apiKey;

import io.hops.hopsworks.common.dao.user.security.apiKey.ApiKey;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiKeyFacade;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiKeyScope;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiKeyScopeFacade;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.security.utils.Secret;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ApiKeyController {
  
  private static final Logger LOGGER = Logger.getLogger(ApiKeyController.class.getName());
  private static final int RETRY_KEY_CREATION = 10;
  
  @EJB
  private ApiKeyFacade apiKeyFacade;
  @EJB
  private ApiKeyScopeFacade apiKeyScopeFacade;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private EmailBean emailBean;
  
  /**
   * Create new key for the give user with the given key name and scopes.
   * @param user
   * @param keyName
   * @param scopes
   * @throws UserException
   * @throws ApiKeyException
   * @return
   */
  public String createNewKey(Users user, String keyName, Set<ApiScope> scopes)
    throws UserException, ApiKeyException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    if (keyName == null || keyName.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NAME_NOT_SPECIFIED, Level.FINE);
    }
    if (keyName.length() > 45) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NAME_NOT_VALID, Level.FINE);
    }
    if (scopes == null || scopes.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_NOT_SPECIFIED, Level.FINE);
    }
    ApiKey apiKey = apiKeyFacade.findByUserAndName(user, keyName);
    if (apiKey != null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NAME_EXIST, Level.FINE);
    }
    Secret secret = generateApiKey();
    Date date = new Date();
    apiKey = new ApiKey(user, secret.getPrefix(), secret.getSha256HexDigest(), secret.getSalt(), date, date, keyName);
    List<ApiKeyScope> keyScopes = getKeyScopes(scopes, apiKey);
    apiKey.setApiKeyScopeCollection(keyScopes);
    apiKeyFacade.save(apiKey);
    sendCreatedEmail(user, keyName, date, scopes);
    return secret.getPrefixPlusSecret();
  }
  
  private Secret generateApiKey() throws ApiKeyException {
    int retry = RETRY_KEY_CREATION;
    Secret secret = securityUtils.generateSecret();
    while ((apiKeyFacade.findByPrefix(secret.getPrefix()) != null || !secret.validateSize()) && (retry-- > 0)) {
      secret = securityUtils.generateSecret();
    }
    if (retry < 1) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_CREATED, Level.SEVERE,
        "Failed to generate unique key prefix after " + RETRY_KEY_CREATION + " retries.");
    }
    return secret;
  }
  
  private List<ApiKeyScope> getKeyScopes(Set<ApiScope> scopes, ApiKey apiKey) {
    List<ApiKeyScope> keyScopes = new ArrayList<>();
    for (ApiScope scope : scopes) {
      keyScopes.add(new ApiKeyScope(scope, apiKey));
    }
    return keyScopes;
  }
  
  /**
   *
   * @param apiKey
   * @return
   */
  public Set<ApiScope> getScopes(ApiKey apiKey) {
    Set<ApiScope> scopes = new HashSet<>();
    for (ApiKeyScope scope : apiKey.getApiKeyScopeCollection()) {
      scopes.add(scope.getScope());
    }
    return scopes;
  }
  
  /**
   *
   * @param key
   * @return
   * @throws ApiKeyException
   */
  public ApiKey getApiKey(String key) throws ApiKeyException {
    String[] parts = key.split(Secret.KEY_ID_SEPARATOR_REGEX);
    if (parts.length < 2) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.FINE);
    }
    ApiKey apiKey = apiKeyFacade.findByPrefix(parts[0]);
    if (apiKey == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.FINE);
    }
    //___MinLength can be set to 0 b/c no validation is needed if the key was in db
    Secret secret = new Secret(parts[0], parts[1], apiKey.getSalt());
    if (!secret.getSha256HexDigest().equals(apiKey.getSecret())) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.FINE);
    }
    return apiKey;
  }
  
  /**
   *
   * @param user
   * @return
   */
  public List<ApiKey> getKeys(Users user) {
    return apiKeyFacade.findByUser(user);
  }
  
  /**
   *
   * @param user
   * @param keyName
   */
  public void deleteKey(Users user, String keyName) {
    ApiKey apiKey = apiKeyFacade.findByUserAndName(user, keyName);
    if (apiKey == null) {
      return;
    }
    apiKeyFacade.remove(apiKey);
    sendDeletedEmail(user, keyName);
  }
  
  /**
   *
   * @param user
   */
  public void deleteAll(Users user) {
    List<ApiKey> keys = apiKeyFacade.findByUser(user);
    for (ApiKey key : keys) {
      apiKeyFacade.remove(key);
    }
    sendDeletedAllEmail(user);
  }
  
  /**
   *
   * @param user
   * @param keyName
   * @param scopes
   * @return
   * @throws ApiKeyException
   */
  public ApiKey addScope(Users user, String keyName, Set<ApiScope> scopes) throws ApiKeyException {
    ApiKey apiKey = validate(user, keyName, scopes);
    List<ApiKeyScope> newScopes;
    Set<ApiScope> oldScopes = toApiScope(apiKey.getApiKeyScopeCollection());
    scopes.removeAll(oldScopes);
    if (!scopes.isEmpty()) {
      newScopes = getKeyScopes(scopes, apiKey);
      apiKey.getApiKeyScopeCollection().addAll(newScopes);
      apiKey.setModified(new Date());
      apiKey = apiKeyFacade.update(apiKey);
    }
    return apiKey;
  }
  
  /**
   *
   * @param user
   * @param keyName
   * @param scopes
   * @return
   * @throws ApiKeyException
   */
  public ApiKey removeScope(Users user, String keyName, Set<ApiScope> scopes) throws ApiKeyException {
    ApiKey apiKey = validate(user, keyName, scopes);
    Collection<ApiKeyScope> oldScopes = apiKey.getApiKeyScopeCollection();
    List<ApiKeyScope> toRemove = new ArrayList<>();
    for (ApiScope scope : scopes) {
      for (ApiKeyScope apiKeyScope : oldScopes) {
        if (apiKeyScope.getScope().equals(scope)) {
          toRemove.add(apiKeyScope);
          break;
        }
      }
    }
    boolean removed = apiKey.getApiKeyScopeCollection().removeAll(toRemove);
    if (removed && !apiKey.getApiKeyScopeCollection().isEmpty()) {
      //this should not be necessary
      for (ApiKeyScope apiKeyScope : toRemove) {
        apiKeyScopeFacade.remove(apiKeyScope);
      }
      apiKey.setModified(new Date());
      apiKey = apiKeyFacade.update(apiKey);
    } else if (removed && apiKey.getApiKeyScopeCollection().isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_EMPTY, Level.FINE);
    }
    return apiKey;
  }
  
  /**
   *
   * @param user
   * @param keyName
   * @param scopes
   * @return
   * @throws ApiKeyException
   */
  public ApiKey update(Users user, String keyName, Set<ApiScope> scopes) throws ApiKeyException {
    ApiKey apiKey = validate(user, keyName, scopes);
    Collection<ApiKeyScope> oldScopes = apiKey.getApiKeyScopeCollection();
    List<ApiKeyScope> toKeep = new ArrayList<>();
    List<ApiKeyScope> toAdd = new ArrayList<>();
    boolean exist;
    boolean added = false;
    for (ApiScope scope : scopes) {
      exist = false;
      for (ApiKeyScope apiKeyScope : oldScopes) {
        if (apiKeyScope.getScope().equals(scope)) {
          toKeep.add(apiKeyScope);
          exist = true;
          break;
        }
      }
      if (!exist) {
        added = true;
        toAdd.add(new ApiKeyScope(scope, apiKey));
      }
    }
    boolean update = false;
    oldScopes.removeAll(toKeep);
    if (!oldScopes.isEmpty()) {
      for (ApiKeyScope apiKeyScope : oldScopes) {
        apiKeyScopeFacade.remove(apiKeyScope);
      }
      update = true;
    }
    if (added) {
      toKeep.addAll(toAdd);
      update = true;
    }
    if (update) {
      apiKey.setApiKeyScopeCollection(toKeep);
      apiKey.setModified(new Date());
      apiKey = apiKeyFacade.update(apiKey);
    }
    return apiKey;
  }
  
  private Set<ApiScope> toApiScope(Collection<ApiKeyScope> scopeSet) {
    Set<ApiScope> scopes = new HashSet<>();
    for (ApiKeyScope apiKeyScope : scopeSet) {
      scopes.add(apiKeyScope.getScope());
    }
    return scopes;
  }
  
  
  private ApiKey validate(Users user, String keyName, Set<ApiScope> scopes) throws ApiKeyException {
    ApiKey apiKey = apiKeyFacade.findByUserAndName(user, keyName);
    if (apiKey == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.FINE);
    }
    if (scopes == null || scopes.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_NOT_SPECIFIED, Level.FINE);
    }
    return apiKey;
  }
  
  private void sendCreatedEmail(Users user, String keyName, Date createdOn, Set<ApiScope> scopes) {
    String subject = UserAccountsEmailMessages.API_KEY_CREATED_SUBJECT;
    String msg = UserAccountsEmailMessages.buildApiKeyCreatedMessage(keyName, createdOn, user.getEmail(), scopes);
    sendEmail(user, subject, msg);
  }
  
  private void sendDeletedEmail(Users user, String keyName) {
    String subject = UserAccountsEmailMessages.API_KEY_DELETED_SUBJECT;
    Date deletedOn = new Date();
    String msg = UserAccountsEmailMessages.buildApiKeyDeletedMessage(keyName, deletedOn, user.getEmail());
    sendEmail(user, subject, msg);
  }
  
  private void sendDeletedAllEmail(Users user) {
    String subject = UserAccountsEmailMessages.API_KEY_DELETED_SUBJECT;
    Date deletedOn = new Date();
    String msg = UserAccountsEmailMessages.buildApiKeyDeletedAllMessage(deletedOn, user.getEmail());
    sendEmail(user, subject, msg);
  }
  
  private void sendEmail(Users user, String subject, String msg) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    try {
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
    } catch (MessagingException e) {
      LOGGER.log(Level.WARNING, "Failed to send api key creation verification email. {0}", e.getMessage());
    }
  }
  
}
