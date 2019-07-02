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
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKey;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeyId;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeyPlaintext;
import io.hops.hopsworks.common.dao.user.security.ThirdPartyApiKeysFacade;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.security.SymmetricEncryptionDescriptor;
import io.hops.hopsworks.common.security.SymmetricEncryptionService;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
/**
 * Stateless bean for managing Third-party API keys of users
 * Keys are encrypted with Hopsworks master encryption password
 * before persisted in the database.
 */
public class ThirdPartyApiKeysController {
  private static final Logger LOG = Logger.getLogger(ThirdPartyApiKeysController.class.getName());
  
  @EJB
  private ThirdPartyApiKeysFacade thirdPartyApiKeysFacade;
  @EJB
  private SymmetricEncryptionService symmetricEncryptionService;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private UserFacade userFacade;
  
  /**
   * Adds a new API key. The key is encrypted before persisted in the database.
   * It throws an exception if a key with the same name already exists for the
   * same user.
   *
   * @param user User to add the key
   * @param keyName Identifier of the key
   * @param key The key itself
   * @throws UserException
   */
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
    try {
      apiKey = new ThirdPartyApiKey(id, encryptKey(key), DateUtils.localDateTime2Date(DateUtils.getNow()));
      thirdPartyApiKeysFacade.persist(apiKey);
    } catch (IOException | GeneralSecurityException ex) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_ENCRYPTION_ERROR, Level.SEVERE,
          "Error encrypting API key", "Could not encrypt API key " + keyName, ex);
    }
  }
  
  /**
   * Gets all the API key names associated with a user. The actually key is not
   * returned, nor decrypted.
   *
   * @param user The user to fetch the API keys for
   * @return A view of all key names associated with the user
   * @throws UserException
   */
  public List<ThirdPartyApiKeyPlaintext> getAllApiKeysForUser(Users user) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    List<ThirdPartyApiKey> keys = thirdPartyApiKeysFacade.findAllForUser(user);
    return keys.stream()
        .map(c -> constructApiKeyView(user, c))
        .collect(Collectors.toList());
  }
  
  /**
   * Deletes an API key associated with a user. It does NOT throw an exception if
   * the key does not exist
   *
   * @param user The user who owns the key
   * @param keyName The name of the key
   * @throws UserException
   */
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
  
  /**
   * Get all API keys that exist in the system encrypted.
   * It is used for handling a Hopsworks master encryption password change
   * @return A list with all API keys in the system encrypted
   */
  public List<ThirdPartyApiKey> getAllCipheredApiKeys() {
    return thirdPartyApiKeysFacade.findAll();
  }
  
  /**
   * Gets a decrypted API key
   * @param user The user associated with the key
   * @param keyName The key identifier
   * @return The API key decrypted along with some metadata
   * @throws UserException
   */
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
    if (key == null) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_EXISTS, Level.FINE,
          "Could not find API key for user",
          "Could not find API key with name " + keyName + " for user " + user.getUsername());
    }
    try {
      return decrypt(user, key);
    } catch (IOException | GeneralSecurityException ex) {
      throw new UserException(RESTCodes.UserErrorCode.THIRD_PARTY_API_KEY_ENCRYPTION_ERROR, Level.SEVERE,
          "Error decrypting API key", "Could not decrypt API key " + keyName, ex);
    }
  }
  
  /**
   * Constructs an API key view without the actual key
   *
   * @param user
   * @param ciphered
   * @return
   */
  private ThirdPartyApiKeyPlaintext constructApiKeyView(Users user, ThirdPartyApiKey ciphered) {
    return ThirdPartyApiKeyPlaintext.newInstance(user, ciphered.getId().getName(), "", ciphered.getAddedOn());
  }
  
  /**
   * Decrypts an encrypted API key
   *
   * @param user
   * @param ciphered
   * @return
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private ThirdPartyApiKeyPlaintext decrypt(Users user, ThirdPartyApiKey ciphered)
      throws IOException, GeneralSecurityException {
    String password = certificatesMgmService.getMasterEncryptionPassword();
  
    // [salt(64),iv(12),payload)]
    byte[][] split = symmetricEncryptionService.splitPayloadFromCryptoPrimitives(ciphered.getKey());
    
    SymmetricEncryptionDescriptor descriptor = new SymmetricEncryptionDescriptor.Builder()
        .setPassword(password)
        .setSalt(split[0])
        .setIV(split[1])
        .setInput(split[2])
        .build();
    descriptor = symmetricEncryptionService.decrypt(descriptor);
    
    byte[] plaintext = descriptor.getOutput();
    
    
    return ThirdPartyApiKeyPlaintext.newInstance(user, ciphered.getId().getName(), bytes2string(plaintext),
        ciphered.getAddedOn());
  }
  
  /**
   * Encrypts an API key.
   *
   * @param key
   * @return Encrypted key along with cryptographic primitives. The structure is the following:
   * Salt(64 bytes), InitializationVector(12 bytes), EncryptedPayload
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private byte[] encryptKey(String key) throws IOException, GeneralSecurityException {
    String password = certificatesMgmService.getMasterEncryptionPassword();
    SymmetricEncryptionDescriptor descriptor = new SymmetricEncryptionDescriptor.Builder()
        .setInput(string2bytes(key))
        .setPassword(password)
        .build();
    descriptor = symmetricEncryptionService.encrypt(descriptor);
    
    return symmetricEncryptionService.mergePayloadWithCryptoPrimitives(descriptor.getSalt(), descriptor.getIv(),
        descriptor.getOutput());
  }
  
  /**
   * Utility method to convert a String to byte array
   * using the system's default charset
   *
   * @param str
   * @return
   */
  private byte[] string2bytes(String str) {
    return str.getBytes(Charset.defaultCharset());
  }
  
  /**
   * Utility method to convert a byte array to String
   * using the system's default charset
   * @param bytes
   * @return
   */
  private String bytes2string(byte[] bytes) {
    return new String(bytes, Charset.defaultCharset());
  }
}
