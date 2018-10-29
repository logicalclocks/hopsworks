/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.ejb.Singleton;

@Singleton
public class SigningKeyGenerator {
  
  private KeyGenerator keyGenerator;
  
  /**
   * Generates a secret key for the given algorithm.
   * @param algorithm
   * @return base64Encoded string
   * @throws NoSuchAlgorithmException 
   */
  public String getSigningKey(String algorithm) throws NoSuchAlgorithmException {
    if (keyGenerator == null || !keyGenerator.getAlgorithm().equals(algorithm)) {
      keyGenerator = KeyGenerator.getInstance(algorithm);
    }
    byte[] keyBytes = keyGenerator.generateKey().getEncoded();
    String base64Encoded = Base64.getEncoder().encodeToString(keyBytes);
    return base64Encoded;
  }
}
