/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.gcs;

import java.io.Serializable;

public class EncryptionSecrets implements Serializable {
  private String encryptionKey;
  private String encryptionKeyHash;
  
  public EncryptionSecrets() {
  }
  
  public EncryptionSecrets(String encryptionKey, String encryptionKeyHash) {
    this.encryptionKey = encryptionKey;
    this.encryptionKeyHash = encryptionKeyHash;
  }
  
  public String getEncryptionKey() {
    return encryptionKey;
  }
  
  public void setEncryptionKey(String encryptionKey) {
    this.encryptionKey = encryptionKey;
  }
  
  public String getEncryptionKeyHash() {
    return encryptionKeyHash;
  }
  
  public void setEncryptionKeyHash(String encryptionKeyHash) {
    this.encryptionKeyHash = encryptionKeyHash;
  }
  
  @Override
  public String toString() {
    return "EncryptionSecrets{" +
      "encryptionKey='" + encryptionKey + '\'' +
      ", encryptionKeyHash='" + encryptionKeyHash + '\'' +
      '}';
  }
}
