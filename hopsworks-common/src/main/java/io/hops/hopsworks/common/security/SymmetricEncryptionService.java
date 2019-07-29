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

package io.hops.hopsworks.common.security;

import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Stateless;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

@Stateless
/**
 * Stateless bean providing symmetric encryption methods
 * The encryption algorithm is AES in GCM mode
 * For key derivation is used PBKDF2WithHmacSHA512 and
 * encryption key is 128 bits long
 */
public class SymmetricEncryptionService {
  
  private static final String RNG_IMPL = "NativePRNGNonBlocking";
  
  // Key constants
  public static final int SALT_LENGTH = 64;
  private static final int KEY_DERIVATION_ITERATIONS = 10000;
  private static final int KEY_SIZE = 128;
  private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA512";
  
  // AES constants
  private static final String ENCRYPTION_ALGORITHM = "AES";
  private static final String AES_MODE = "AES/GCM/PKCS5Padding";
  private static final int GCM_AUTHENTICATION_TAG_SIZE = 128;
  public static final int IV_LENGTH = 12;
  
  SecureRandom rand;
  
  @PostConstruct
  public void init() {
    try {
      // True randomness requires a blocking RNG
      rand = SecureRandom.getInstance(RNG_IMPL);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  /**
   * Encrypts data using supplied cryptographic primitives. If salt or initialization vector
   * is not provided, random are generated. Salt should be @SALT_LENGTH bytes and Initialization
   * Vector @IV_LENGTH bytes
   *
   * Salt and IV should be stored along with the ciphertext in order to decrypt it. IV should be
   * absolutely unique for each message encrypted with the same key!
   *
   * @param descriptor Descriptor containing the message to be encrypted and crypto primitives
   * @return Descriptor containing the output of encryption and the crypto primitives used
   * @throws GeneralSecurityException
   */
  public SymmetricEncryptionDescriptor encrypt(SymmetricEncryptionDescriptor descriptor)
      throws GeneralSecurityException {
    byte[] salt, iv;
    if (descriptor.getSalt() != null) {
      salt = descriptor.getSalt();
    } else {
      salt = new byte[SALT_LENGTH];
      generateRandom(salt);
    }
    if (descriptor.getIv() != null) {
      iv = descriptor.getIv();
    } else {
      iv = new byte[IV_LENGTH];
      generateRandom(iv);
    }
    
    Pair<KeySpec, SecretKey> keyMaterial = buildSecretKey(descriptor.getPassword(), salt);
    
    Cipher cipher = getCipher();
    cipher.init(Cipher.ENCRYPT_MODE, keyMaterial.getRight(), getGCMSpec(iv));
    byte[] ciphertext = cipher.doFinal(descriptor.getInput());
    
    clearPasswords(keyMaterial.getLeft(), descriptor);
    
    return new SymmetricEncryptionDescriptor.Builder()
        .setOutput(ciphertext)
        .setSalt(salt)
        .setIV(iv)
        .build();
  }
  
  /**
   * Decrypts data using the cryptographic primitives supplied. Salt and IV should be the same
   * as the ones used for encryption.
   *
   * @param descriptor Descriptor which should contain the encrypted payload, the password used
   *                   to derive the key, salt and initialization vector used during encryption
   * @return Descriptor containing the plaintext message
   * @throws GeneralSecurityException
   */
  public SymmetricEncryptionDescriptor decrypt(SymmetricEncryptionDescriptor descriptor)
      throws GeneralSecurityException {
    if (descriptor.getSalt() == null || descriptor.getIv() == null || descriptor.getPassword() == null) {
      throw new IllegalArgumentException("Cryptographic primitives are empty");
    }
    Pair<KeySpec, SecretKey> keyMaterial = buildSecretKey(descriptor.getPassword(), descriptor.getSalt());
    
    Cipher cipher = getCipher();
    cipher.init(Cipher.DECRYPT_MODE, keyMaterial.getRight(), getGCMSpec(descriptor.getIv()));
    byte[] plaintext = cipher.doFinal(descriptor.getInput());
    
    clearPasswords(keyMaterial.getLeft(), descriptor);
    return new SymmetricEncryptionDescriptor.Builder()
        .setOutput(plaintext)
        .build();
  }
  
  /**
   * Utility method which merges Salt, IV and encrypted payload into one byte array
   * [salt(64 bytes), iv(12 bytes), payload]
   * @param salt
   * @param iv
   * @param payload
   * @return
   */
  public byte[] mergePayloadWithCryptoPrimitives(byte[] salt, byte[] iv, byte[] payload) {
    byte[] payloadWithPrimitives = new byte[salt.length + iv.length + payload.length];
    System.arraycopy(salt, 0, payloadWithPrimitives, 0, salt.length);
    System.arraycopy(iv, 0, payloadWithPrimitives, salt.length, iv.length);
    System.arraycopy(payload, 0, payloadWithPrimitives, salt.length + iv.length, payload.length);
    return payloadWithPrimitives;
  }
  
  /**
   * Utility method which splits salt, IV and payload. It assumes salt is 64 bytes long,
   * IV is 12 bytes and the rest is the message.
   * @param payloadWithCryptoPrimitives
   * @return An array where the first element is the Salt, the second is the IV and
   * the third is the message
   */
  public byte[][] splitPayloadFromCryptoPrimitives(byte[] payloadWithCryptoPrimitives) {
    byte[] salt = new byte[SALT_LENGTH];
    byte[] iv = new byte[IV_LENGTH];
    byte[] ciphertext = new byte[payloadWithCryptoPrimitives.length - SALT_LENGTH - IV_LENGTH];
  
    System.arraycopy(payloadWithCryptoPrimitives, 0, salt, 0, salt.length);
    System.arraycopy(payloadWithCryptoPrimitives, salt.length, iv, 0, iv.length);
    System.arraycopy(payloadWithCryptoPrimitives, salt.length + iv.length, ciphertext, 0, ciphertext.length);
    byte[][] splitPayload = new byte[3][];
    splitPayload[0] = salt;
    splitPayload[1] = iv;
    splitPayload[2] = ciphertext;
    
    return splitPayload;
  }
  
  /**
   * Clear the password from the key specification. Call this only after you are done
   * with the encryption/decryption
   * @param keySpec
   * @param descriptor
   */
  private void clearPasswords(KeySpec keySpec, SymmetricEncryptionDescriptor descriptor) {
    if (keySpec instanceof PBEKeySpec) {
      ((PBEKeySpec) keySpec).clearPassword();
    }
    descriptor.clearPassword();
  }
  
  /**
   * Generate a secret key using @KEY_DERIVATION_ALGORITHM algorithm
   *
   * @param password Password to use
   * @param salt Salt for the key derivation function
   * @return A pair of the key specification and the key itself
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  private Pair<KeySpec, SecretKey> buildSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException,
      InvalidKeySpecException {
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
    KeySpec keySpec = new PBEKeySpec(password, salt, KEY_DERIVATION_ITERATIONS, KEY_SIZE);
    SecretKey key = secretKeyFactory.generateSecret(keySpec);
    return Pair.of(keySpec, new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM));
  }
  
  private Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
    return Cipher.getInstance(AES_MODE);
  }
  
  private GCMParameterSpec getGCMSpec(byte[] iv) {
    return new GCMParameterSpec(GCM_AUTHENTICATION_TAG_SIZE, iv);
  }
  
  /**
   * Generate random data.
   * Caution: It does not block for true random!
   *
   * @param buffer Buffer to place the random data
   */
  private void generateRandom(byte[] buffer) {
    rand.nextBytes(buffer);
  }
}
