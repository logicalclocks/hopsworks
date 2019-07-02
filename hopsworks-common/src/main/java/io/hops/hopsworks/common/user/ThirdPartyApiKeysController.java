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

package io.hops.hopsworks.common.user;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKey;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeyId;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeyPlaintext;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeysFacade;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ThirdPartyApiKeysController {
  private static final Logger LOG = Logger.getLogger(ThirdPartyApiKeysController.class.getName());
  
  @EJB
  private ThirdPartyApiKeysFacade thirdPartyApiKeysFacade;
  
  public void addApiKey(Users user, String keyName, String key) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    if (Strings.isNullOrEmpty(keyName) || Strings.isNullOrEmpty(key)) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_EMPTY, Level.FINE,
          "Third party API key is either null or empty", "3rd party API key name or key is empty or null");
    }
    ThirdPartyApiKeyId id = new ThirdPartyApiKeyId(user.getUid(), keyName);
    ThirdPartyApiKey apiKey = thirdPartyApiKeysFacade.findById(id);
    if (apiKey != null) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_EXISTS, Level.FINE,
          "API key already exists", "API key with name " + keyName + " already exists for user " + user.getUsername());
    }
    apiKey = new ThirdPartyApiKey(id, string2bytes(key), DateUtils.localDateTime2Date(DateUtils.getNow()));
    thirdPartyApiKeysFacade.persist(apiKey);
  }
  
  public List<ThirdPartyApiKeyPlaintext> getAllApiKeysForUser(Users user) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    List<ThirdPartyApiKey> keys = thirdPartyApiKeysFacade.findAllForUser(user);
    return keys.stream()
        .map(c -> decrypt(user, c))
        .collect(Collectors.toList());
  }
  
  public void deleteApiKey(Users user, String keyName) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    if (Strings.isNullOrEmpty(keyName)) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_EMPTY, Level.FINE,
          "Third party API key is either null or empty", "3rd party API key name or key is empty or null");
    }
    ThirdPartyApiKeyId keyId = new ThirdPartyApiKeyId(user.getUid(), keyName);
    thirdPartyApiKeysFacade.deleteKey(keyId);
  }
  
  public ThirdPartyApiKeyPlaintext getApiKey(Users user, String keyName) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    if (Strings.isNullOrEmpty(keyName)) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_EMPTY, Level.FINE,
          "Third party API key is either null or empty", "3rd party API key name or key is empty or null");
    }
    ThirdPartyApiKeyId id = new ThirdPartyApiKeyId(user.getUid(), keyName);
    ThirdPartyApiKey key = thirdPartyApiKeysFacade.findById(id);
    return decrypt(user, key);
  }
  
  private ThirdPartyApiKeyPlaintext decrypt(Users user, ThirdPartyApiKey ciphered) {
    return ThirdPartyApiKeyPlaintext.newInstance(user, ciphered.getId().getName(), bytes2string(ciphered.getKey()),
        ciphered.getAddedOn());
  }
  
  private byte[] string2bytes(String str) {
    return str.getBytes(Charset.defaultCharset());
  }
  
  private String bytes2string(byte[] bytes) {
    return new String(bytes, Charset.defaultCharset());
  }
}
