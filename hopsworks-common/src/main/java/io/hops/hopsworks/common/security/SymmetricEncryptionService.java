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
  
  public SymmetricEncryptionDescriptor encrypt(SymmetricEncryptionDescriptor descriptor)
      throws GeneralSecurityException {
    byte[] salt = descriptor.getSalt() != null ? descriptor.getSalt() : generateRandom(new byte[SALT_LENGTH]);
    byte[] iv = descriptor.getIv() != null ? descriptor.getIv() : generateRandom(new byte[IV_LENGTH]);
    
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
  
  private void clearPasswords(KeySpec keySpec, SymmetricEncryptionDescriptor descriptor) {
    if (keySpec instanceof PBEKeySpec) {
      ((PBEKeySpec) keySpec).clearPassword();
    }
    descriptor.clearPassword();
  }
  
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
  
  private byte[] generateRandom(byte[] buffer) {
    rand.nextBytes(buffer);
    return buffer;
  }
}
