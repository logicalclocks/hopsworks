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

import java.nio.charset.Charset;

/**
 * Container class used by @{@link SymmetricEncryptionService}. It contains cryptographic
 * primitives such as salt and initialization vector, the password to derive the secret key
 * and depending on the operation input for encryption and output from decryption.
 */
public class SymmetricEncryptionDescriptor {
  private final byte[] input;
  private char[] password;
  private final byte[] salt;
  private final byte[] iv;
  private final byte[] output;
  
  private SymmetricEncryptionDescriptor(Builder builder) {
    this.input = builder.input;
    this.password = builder.password;
    this.salt = builder.salt;
    this.iv = builder.iv;
    this.output = builder.output;
  }
  
  /**
   * Input for encryption
   * @return
   */
  public byte[] getInput() {
    return input;
  }
  
  /**
   * Password to derive secret key
   * @return
   */
  public char[] getPassword() {
    return password;
  }
  
  /**
   * Salt used to derive the key
   * @return
   */
  public byte[] getSalt() {
    return salt;
  }
  
  /**
   * Initialization vector for AES
   * @return
   */
  public byte[] getIv() {
    return iv;
  }
  
  /**
   * Output of decryption
   * @return
   */
  public byte[] getOutput() {
    return output;
  }
  
  /**
   * Clear password.
   * Caution: You should clear the password only after encryption or decryption has happened
   */
  public void clearPassword() {
    if (password != null) {
      for (int i = 0; i < password.length; i++) {
        password[i] = ' ';
      }
      password = null;
    }
  }
  
  public static class Builder {
    private byte[] input;
    private char[] password;
    private byte[] salt;
    private byte[] iv;
    private byte[] output;
    
    public Builder() {}
  
    /**
     * Set the input for encryption. It can be omitted if the operation is decryption
     * @param input Input for encryption
     * @return
     */
    public Builder setInput(byte[] input) {
      this.input = input;
      return this;
    }
  
    /**
     * Set the input for encryption. It can be omitted if the operation is decryption
     * @param input Input for encryption
     * @return
     */
    public Builder setInput(String input) {
      this.input = input.getBytes(Charset.defaultCharset());
      return this;
    }
  
    /**
     * The password to derive the secret key for encryption/decryption
     * @param password
     * @return
     */
    public Builder setPassword(char[] password) {
      this.password = password;
      return this;
    }
  
    /**
     * The password to derive the secret key for encryption/decryption
     * @param password
     * @return
     */
    public Builder setPassword(String password) {
      this.password = password.toCharArray();
      return this;
    }
  
    /**
     * Salt to derive the key. For encryption it can be omitted, random value will be generated.
     * It is necessary during decryption
     * @param salt
     * @return
     */
    public Builder setSalt(byte[] salt) {
      this.salt = salt;
      return this;
    }
  
    /**
     * Initialization Vector to be used by AES. For encryption it can be omitted,
     * random value will be generated.
     * It is necessary during decryption
     * @param iv
     * @return
     */
    public Builder setIV(byte[] iv) {
      this.iv = iv;
      return this;
    }
  
    /**
     * Set the output after decryption. It can be omitted if the operation is encryption
     * @param output
     * @return
     */
    public Builder setOutput(byte[] output) {
      this.output = output;
      return this;
    }
    
    public SymmetricEncryptionDescriptor build() {
      return new SymmetricEncryptionDescriptor(this);
    }
  }
}
