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
package io.hops.hopsworks.common.security.utils;

import io.hops.hopsworks.common.user.UserValidator;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang.RandomStringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SecurityUtils {
  private static final Logger LOGGER = Logger.getLogger(SecurityUtils.class.getName());
  private final int RANDOM_KEY_LEN = 64;
  private final int RANDOM_PREFIX_KEY_LEN = 16;
  public final static long RESET_LINK_VALID_FOR_HOUR = 24;
  private SecureRandom secureRandom;
  
  @PostConstruct
  public void init() {
    secureRandom = new SecureRandom();
  }
  
  /**
   * Generate the secrete key for mobile devices.
   *
   * @return
   */
  public String calculateSecretKey() {
    byte[] encodedKey = calculateSecretKey(10);
    return new String(encodedKey);
  }
  
  public byte[] calculateSecretKey(int length) {
    byte[] bytes = new byte[length];
    secureRandom.nextBytes(bytes);
    Base32 codec = new Base32();
    return codec.encode(bytes);
  }
  
  /**
   * Generate secure random with the given length
   *
   * @param length
   * @return Base64 encoded secure random UTF-8 string
   */
  public String generateSecureRandomString(int length) {
    byte[] encoded = generateSecureRandom(length);
    return new String(encoded, StandardCharsets.UTF_8);
  }
  
  /**
   * Generate secure random with default length.
   *
   * @return Base64 encoded secure random UTF-8 string
   */
  public String generateSecureRandomString() {
    return generateSecureRandomString(RANDOM_KEY_LEN);
  }
  
  /**
   * Base64 encoded secure random
   *
   * @param length
   * @return
   */
  public byte[] generateSecureRandom(int length) {
    byte[] bytes = new byte[length];
    secureRandom.nextBytes(bytes);
    byte[] encoded = Base64.getEncoder().encode(bytes);
    return encoded;
  }
  
  public String generateRandomString(int length) {
    return RandomStringUtils.random(length, 0, 0, true, true, null, secureRandom);
  }
  
  /**
   * Generate key and salt with default length.
   *
   * @return
   */
  public Secret generateSecret() {
    String id = generateRandomString(RANDOM_PREFIX_KEY_LEN);
    String key = generateRandomString(RANDOM_KEY_LEN);
    String salt = generateSecureRandomString(RANDOM_KEY_LEN);
    Secret secret = new Secret(id, key, salt, RANDOM_PREFIX_KEY_LEN, RANDOM_KEY_LEN, RANDOM_KEY_LEN);
    return secret;
  }
  
  /**
   *
   * @param password
   * @return
   */
  public Secret generateSecret(String password) {
    String salt = generateSecureRandomString(RANDOM_KEY_LEN);
    Secret secret = new Secret(password, salt, UserValidator.PASSWORD_MIN_LENGTH, RANDOM_KEY_LEN);
    return secret;
  }
  
  /**
   *
   * @param key
   * @return
   */
  public String urlEncode(String key) {
    String urlEncoded;
    try {
      urlEncoded = URLEncoder.encode(key, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
    return urlEncoded;
  }
}
