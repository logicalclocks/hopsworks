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

import java.io.Serializable;

public class FeaturestoreS3ConnectorAccessAndSecretKey implements Serializable {
  private String accessKey;
  private String secretKey;
  
  public FeaturestoreS3ConnectorAccessAndSecretKey(String accessKey, String secretKey) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }
  
  public FeaturestoreS3ConnectorAccessAndSecretKey() { }
  
  public String getAccessKey() {
    return accessKey;
  }
  
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }
  
  public String getSecretKey() {
    return secretKey;
  }
  
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreS3ConnectorAccessAndSecretKey{" +
      "accessKey='" + accessKey + '\'' +
      ", secretKey='" + secretKey + '\'' +
      '}';
  }
}
