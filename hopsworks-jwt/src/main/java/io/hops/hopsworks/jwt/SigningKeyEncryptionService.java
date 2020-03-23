/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt;

import io.hops.hopsworks.persistence.entity.jwt.JwtSigningKey;
import io.hops.hopsworks.security.encryption.SymmetricEncryptionDescriptor;
import io.hops.hopsworks.security.encryption.SymmetricEncryptionService;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.SigningKeyEncryptionException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.security.password.MasterPasswordService;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class SigningKeyEncryptionService {
  private final static Logger LOGGER = Logger.getLogger(SigningKeyEncryptionService.class.getName());
  
  private ConcurrentHashMap<Integer, DecryptedSigningKey> signingKeys = new ConcurrentHashMap<>();
  @EJB
  private JwtSigningKeyFacade jwtSigningKeyFacade;
  @EJB
  private SigningKeyGenerator signingKeyGenerator;
  @EJB
  private SymmetricEncryptionService symmetricEncryptionService;
  @EJB
  private MasterPasswordService masterPasswordService;
  
  public SigningKeyEncryptionService() {
  }
  
  //for test
  public SigningKeyEncryptionService(JwtSigningKeyFacade jwtSigningKeyFacade, SigningKeyGenerator signingKeyGenerator
    , SymmetricEncryptionService symmetricEncryptionService, MasterPasswordService masterPasswordService) {
    this.jwtSigningKeyFacade = jwtSigningKeyFacade;
    this.signingKeyGenerator = signingKeyGenerator;
    this.symmetricEncryptionService = symmetricEncryptionService;
    this.masterPasswordService = masterPasswordService;
  }
  
  /**
   * Clears decrypted signing keys from the cache.
   */
  public void invalidateCache() {
    if (signingKeys != null && !signingKeys.isEmpty()) {
      signingKeys.clear();
      LOGGER.log(Level.INFO, "Decrypted signing key cache cleared.");
    }
  }
  
  public DecryptedSigningKey removeFromCache(JwtSigningKey jwtSigningKey) {
    return signingKeys.remove(jwtSigningKey.getId());
  }
  
  private DecryptedSigningKey decryptAndSave(JwtSigningKey jwtSigningKey) throws SigningKeyEncryptionException {
    byte[] decryptedSecret = decrypt(jwtSigningKey.getSecret());
    return save(decryptedSecret, jwtSigningKey);
  }
  
  private DecryptedSigningKey save(byte[] decryptedSecret, JwtSigningKey jwtSigningKey) {
    DecryptedSigningKey decryptedSigningKey = new DecryptedSigningKey(jwtSigningKey, decryptedSecret);
    signingKeys.put(jwtSigningKey.getId(), decryptedSigningKey);
    return decryptedSigningKey;
  }
  
  /**
   * Get DecryptedSigningKey from cache if it exists else gets it from database
   * @param id
   * @return
   */
  public DecryptedSigningKey getSigningKey(Integer id) throws SigningKeyEncryptionException,
    SigningKeyNotFoundException {
    DecryptedSigningKey decryptedSigningKey = signingKeys.get(id);
    if (decryptedSigningKey != null) {
      return decryptedSigningKey;
    }
    return getSigningKeyFromDb(id);
  }
  
  public DecryptedSigningKey getSigningKey(JwtSigningKey jwtSigningKey) throws SigningKeyEncryptionException {
    DecryptedSigningKey decryptedSigningKey = signingKeys.get(jwtSigningKey.getId());
    if (decryptedSigningKey != null) {
      return decryptedSigningKey;
    }
    return decryptAndSave(jwtSigningKey);
  }
  
  /**
   * Get SigningKey from database and decrypt.
   * @param id
   * @return
   */
  public DecryptedSigningKey getSigningKeyFromDb(Integer id) throws SigningKeyEncryptionException,
    SigningKeyNotFoundException {
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.find(id);
    if (jwtSigningKey == null) {
      throw new SigningKeyNotFoundException("Signing key not found.");
    }
    return decryptAndSave(jwtSigningKey);
  }
  
  /**
   * Create new signing key and saves it encrypted with the master password.
   * @param keyName
   * @param alg
   * @return
   * @throws DuplicateSigningKeyException
   * @throws NoSuchAlgorithmException
   */
  public DecryptedSigningKey createSigningKey(String keyName, SignatureAlgorithm alg)
    throws DuplicateSigningKeyException, NoSuchAlgorithmException, SigningKeyEncryptionException {
    JwtSigningKey signingKey = jwtSigningKeyFacade.findByName(keyName);
    if (signingKey != null) {
      throw new DuplicateSigningKeyException("A signing key with the same name already exists.");
    }
    return createNewSigningKey(keyName, alg);
  }
  
  /**
   * Gets signing key by name if it exists else creates a new and saves it encrypted.
   * @param keyName
   * @param alg
   * @return
   * @throws NoSuchAlgorithmException
   */
  public DecryptedSigningKey getOrCreateSigningKey(String keyName, SignatureAlgorithm alg)
    throws NoSuchAlgorithmException, SigningKeyEncryptionException {
    JwtSigningKey signingKey = jwtSigningKeyFacade.findByName(keyName);
    if (signingKey == null) {
      return createNewSigningKey(keyName, alg);
    }
    return getSigningKey(signingKey);
  }
  
  private DecryptedSigningKey createNewSigningKey(String keyName, SignatureAlgorithm alg)
    throws NoSuchAlgorithmException, SigningKeyEncryptionException {
    byte[] signingKey = signingKeyGenerator.getSigningKey(alg.getJcaName());
    String encryptedSecret = encrypt(signingKey);
    JwtSigningKey jwtSigningKey = new JwtSigningKey(encryptedSecret, keyName);
    jwtSigningKeyFacade.persist(jwtSigningKey);
    jwtSigningKey = jwtSigningKeyFacade.findByName(keyName);
    return save(signingKey, jwtSigningKey);
  }
  
  private byte[] decrypt(String secret) throws SigningKeyEncryptionException {
    try {
      //get master password and decrypt
      String password = masterPasswordService.getMasterEncryptionPassword();
      return decrypt(secret, password);
    } catch (IOException e) {
      throw new SigningKeyEncryptionException("Failed to decrypt signing key. ", e);
    }
  }
  
  public byte[] decrypt(String secret, String masterPassword) throws SigningKeyEncryptionException {
    try {
      // [salt(64),iv(12),payload)]
      byte[][] split = symmetricEncryptionService.splitPayloadFromCryptoPrimitives(Base64.getDecoder().decode(secret));
      SymmetricEncryptionDescriptor descriptor = new SymmetricEncryptionDescriptor.Builder()
        .setPassword(masterPassword)
        .setSalt(split[0])
        .setIV(split[1])
        .setInput(split[2])
        .build();
      descriptor = symmetricEncryptionService.decrypt(descriptor);
      return  descriptor.getOutput();
    } catch (GeneralSecurityException e) {
      throw new SigningKeyEncryptionException("Failed to decrypt signing key. ", e);
    }
  }
  
  private String encrypt(byte[] secret) throws SigningKeyEncryptionException {
    try {
      String password = masterPasswordService.getMasterEncryptionPassword();
      return encrypt(secret, password);
    } catch (IOException e) {
      throw new SigningKeyEncryptionException("Failed to encrypt signing key. ", e);
    }
  }
  
  public String encrypt(byte[] secret, String masterPassword) throws SigningKeyEncryptionException {
    try {
      SymmetricEncryptionDescriptor descriptor = new SymmetricEncryptionDescriptor.Builder()
        .setInput(secret)
        .setPassword(masterPassword)
        .build();
      descriptor = symmetricEncryptionService.encrypt(descriptor);
      byte[] encrypted =
        symmetricEncryptionService.mergePayloadWithCryptoPrimitives(descriptor.getSalt(), descriptor.getIv(),
          descriptor.getOutput());
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (GeneralSecurityException e) {
      throw new SigningKeyEncryptionException("Failed to encrypt signing key. ", e);
    }
  }
  
  public String getNewEncryptedSecret(String secret, String oldMasterPassword, String newMasterPassword)
    throws SigningKeyEncryptionException {
    return encrypt(decrypt(secret, oldMasterPassword), newMasterPassword);
  }
  
}
