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

package io.hops.hopsworks.api.featurestore.json;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO representing the JSON payload from REST-clients when creating/updating S3 storage backends in the featurestore
 */
@XmlRootElement
public class S3ConnectorJsonDTO extends StorageConnectorJsonDTO {
  
  private String s3Bucket;
  private String s3SecretKey;
  private String s3AccessKey;
  

  public S3ConnectorJsonDTO() {
  }
  
  public S3ConnectorJsonDTO(String s3Bucket, String s3SecretKey, String s3AccessKey) {
    this.s3Bucket = s3Bucket;
    this.s3SecretKey = s3SecretKey;
    this.s3AccessKey = s3AccessKey;
  }
  
  public S3ConnectorJsonDTO(String name, String description, String s3Bucket, String s3SecretKey,
    String s3AccessKey) {
    super(name, description);
    this.s3Bucket = s3Bucket;
    this.s3SecretKey = s3SecretKey;
    this.s3AccessKey = s3AccessKey;
  }
  
  @XmlElement
  public String getS3Bucket() {
    return s3Bucket;
  }
  
  public void setS3Bucket(String s3Bucket) {
    this.s3Bucket = s3Bucket;
  }
  
  @XmlElement
  public String getS3SecretKey() {
    return s3SecretKey;
  }
  
  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }
  
  @XmlElement
  public String getS3AccessKey() {
    return s3AccessKey;
  }
  
  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }
}
