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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.s3;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a S3 connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"accessKey", "secretKey", "bucket", "featurestoreId", "name", "description", "id"})
public class FeaturestoreS3ConnectorDTO {
  
  private String accessKey;
  private String secretKey;
  private String bucket;
  private Integer featurestoreId;
  private String name;
  private String description;
  private Integer id;
  
  public FeaturestoreS3ConnectorDTO() {
  }
  
  public FeaturestoreS3ConnectorDTO(FeaturestoreS3Connector featurestoreS3Connector) {
    this.featurestoreId = featurestoreS3Connector.getId();
    this.accessKey = featurestoreS3Connector.getAccessKey();
    this.secretKey = featurestoreS3Connector.getSecretKey();
    this.bucket = featurestoreS3Connector.getBucket();
    this.name = featurestoreS3Connector.getName();
    this.description = featurestoreS3Connector.getDescription();
    this.id = featurestoreS3Connector.getId();
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
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
  
  @XmlElement
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @XmlElement
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  @XmlElement
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreS3ConnectorDTO{" +
      "accessKey='" + accessKey + '\'' +
      ", secretKey='" + secretKey + '\'' +
      ", bucket='" + bucket + '\'' +
      ", featurestoreId=" + featurestoreId +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", id=" + id +
      '}';
  }
}
