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
  
  public byte[] getInput() {
    return input;
  }
  
  public char[] getPassword() {
    return password;
  }
  
  public byte[] getSalt() {
    return salt;
  }
  
  public byte[] getIv() {
    return iv;
  }
  
  public byte[] getOutput() {
    return output;
  }
  
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
    
    public Builder setInput(byte[] input) {
      this.input = input;
      return this;
    }
    
    public Builder setInput(String input) {
      this.input = input.getBytes(Charset.defaultCharset());
      return this;
    }
    
    public Builder setPassword(char[] password) {
      this.password = password;
      return this;
    }
    
    public Builder setPassword(String password) {
      this.password = password.toCharArray();
      return this;
    }
    
    public Builder setSalt(byte[] salt) {
      this.salt = salt;
      return this;
    }
    
    public Builder setIV(byte[] iv) {
      this.iv = iv;
      return this;
    }
    
    public Builder setOutput(byte[] output) {
      this.output = output;
      return this;
    }
    
    public SymmetricEncryptionDescriptor build() {
      return new SymmetricEncryptionDescriptor(this);
    }
  }
}
