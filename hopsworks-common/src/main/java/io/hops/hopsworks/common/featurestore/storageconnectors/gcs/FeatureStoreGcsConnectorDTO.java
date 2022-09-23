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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.gcs.EncryptionAlgorithm;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonTypeInfo(
  use=JsonTypeInfo.Id.NAME,
  include=JsonTypeInfo.As.PROPERTY,
  property="type")
@JsonTypeName("featureStoreGcsConnectorDTO")
public class FeatureStoreGcsConnectorDTO extends FeaturestoreStorageConnectorDTO {

  private String keyPath;
  private EncryptionAlgorithm algorithm;
  private String encryptionKey;
  private String encryptionKeyHash;
  private String bucket;

  public FeatureStoreGcsConnectorDTO() {
  }

  public FeatureStoreGcsConnectorDTO(FeaturestoreConnector featureStoreConnector) {
    super(featureStoreConnector);
    this.algorithm = featureStoreConnector.getGcsConnector().getAlgorithm();
  }

  public String getKeyPath() {
    return keyPath;
  }

  public void setKeyPath(String keyPath) {
    this.keyPath = keyPath;
  }
  
  public EncryptionAlgorithm getAlgorithm() {
    return algorithm;
  }
  
  public void setAlgorithm(EncryptionAlgorithm algorithm) {
    this.algorithm = algorithm;
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
  
  public String getBucket() {
    return bucket;
  }
  
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }
}
