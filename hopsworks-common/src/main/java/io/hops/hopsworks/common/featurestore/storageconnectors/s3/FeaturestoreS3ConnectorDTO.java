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

package io.hops.hopsworks.common.featurestore.storageconnectors.s3;

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3ConnectorEncryptionAlgorithm;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a S3 connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
public class FeaturestoreS3ConnectorDTO extends FeaturestoreStorageConnectorDTO {
  
  private String accessKey;
  private String secretKey;
  private String bucket;
  private String serverEncryptionAlgorithm;
  private String serverEncryptionKey;
  private String iamRole;
  private String sessionToken;
  
  public FeaturestoreS3ConnectorDTO() {
  }

  public FeaturestoreS3ConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
    this.bucket = featurestoreConnector.getS3Connector().getBucket();
    this.serverEncryptionAlgorithm =
        getEncryptionAlgorithmName(featurestoreConnector.getS3Connector().getServerEncryptionAlgorithm());
    this.serverEncryptionKey = featurestoreConnector.getS3Connector().getServerEncryptionKey();
    this.iamRole = featurestoreConnector.getS3Connector().getIamRole();
  }
  
  private String getEncryptionAlgorithmName(FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm) {
    if (serverEncryptionAlgorithm == null) {
      return  null;
    } else {
      return serverEncryptionAlgorithm.getAlgorithm();
    }
  }
  
  @XmlElement
  public String getAccessKey() {
    return accessKey;
  }
  
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }
  
  @XmlElement
  public String getSecretKey() {
    return secretKey;
  }
  
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
  
  @XmlElement
  public String getBucket() {
    return bucket;
  }
  
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @XmlElement
  public String getServerEncryptionAlgorithm() { return serverEncryptionAlgorithm; }

  public void setServerEncryptionAlgorithm(String serverEncryptionAlgorithm) {
    this.serverEncryptionAlgorithm = serverEncryptionAlgorithm;
  }

  @XmlElement
  public String getServerEncryptionKey() { return serverEncryptionKey; }

  public void setServerEncryptionKey(String serverEncryptionKey) { this.serverEncryptionKey = serverEncryptionKey; }

  @XmlElement
  public String getIamRole() {
    return iamRole;
  }

  public void setIamRole(String iamRole) {
    this.iamRole = iamRole;
  }

  @XmlElement
  public String getSessionToken() {
    return sessionToken;
  }

  public void setSessionToken(String sessionToken) {
    this.sessionToken = sessionToken;
  }

  @Override
  public String toString() {
    return "FeaturestoreS3ConnectorDTO{" +
            "accessKey='" + accessKey + '\'' +
            ", secretKey='" + secretKey + '\'' +
            ", bucket='" + bucket + '\'' +
            ", serverEncryptionAlgorithm='" + serverEncryptionAlgorithm + '\'' +
            ", serverEncryptionKey='" + serverEncryptionKey + '\'' +
            '}';
  }
}
