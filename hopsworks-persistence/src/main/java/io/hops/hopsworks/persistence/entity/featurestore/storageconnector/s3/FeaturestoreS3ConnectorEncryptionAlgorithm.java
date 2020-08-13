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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3;

import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Arrays;
import java.util.Optional;

public enum FeaturestoreS3ConnectorEncryptionAlgorithm {
  @XmlEnumValue("AES256")
  AES256("AES256", "Server-Side Encryption with Amazon S3-Managed Keys (SSE-S3)", false),
  @XmlEnumValue("SSE-KMS")
  SSE_KMS("SSE-KMS", "Server-Encryption with AWS KMS-Managed Keys (SSE-KMS)", true);
  
  private String algorithm;
  private String description;
  private boolean requiresKey;
  
  FeaturestoreS3ConnectorEncryptionAlgorithm(String algorithm, String description, boolean requiresKey) {
    this.algorithm = algorithm;
    this.description = description;
    this.requiresKey = requiresKey;
  }
  
  public String getAlgorithm() {
    return algorithm;
  }
  
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }
  
  public String getDescription() { return description; }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public boolean isRequiresKey() {
    return requiresKey;
  }
  
  public void setRequiresKey(boolean requiresKey) {
    this.requiresKey = requiresKey;
  }
  
  public static FeaturestoreS3ConnectorEncryptionAlgorithm fromValue(String s)
    throws IllegalArgumentException{
    Optional<FeaturestoreS3ConnectorEncryptionAlgorithm> algorithm =
      Arrays.asList(FeaturestoreS3ConnectorEncryptionAlgorithm.values())
        .stream().filter(a -> a.getAlgorithm().equals(s)).findFirst();
    
    if (algorithm.isPresent()) {
      return algorithm.get();
    } else {
      throw new IllegalArgumentException("Invalid encryption algorithm provided");
    }
  }
  
  @Override
  public String toString() {
    return algorithm;
  }
}
