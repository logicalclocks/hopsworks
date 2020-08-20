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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the feature_store_s3_connector table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_s3_connector", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreS3Connector.findAll", query = "SELECT fss FROM FeaturestoreS3Connector fss"),
    @NamedQuery(name = "FeaturestoreS3Connector.findById",
        query = "SELECT fss FROM FeaturestoreS3Connector fss WHERE fss.id = :id"),
    @NamedQuery(name = "FeaturestoreS3Connector.findByFeaturestore", query = "SELECT fss " +
        "FROM FeaturestoreS3Connector fss WHERE fss.featurestore = :featurestore"),
    @NamedQuery(name = "FeaturestoreS3Connector.findByFeaturestoreAndId", query = "SELECT fss " +
        "FROM FeaturestoreS3Connector fss WHERE fss.featurestore = :featurestore AND fss.id = :id")})
public class FeaturestoreS3Connector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  private Featurestore featurestore;
  @Column(name = "bucket")
  private String bucket;
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @Column(name = "server_encryption_algorithm")
  @Enumerated(EnumType.ORDINAL)
  private FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm;
  @Column(name = "server_encryption_key")
  private String serverEncryptionKey;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featurestore getFeaturestore() {
    return featurestore;
  }
  
  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }
  
  public String getBucket() {
    return bucket;
  }
  
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public FeaturestoreS3ConnectorEncryptionAlgorithm getServerEncryptionAlgorithm() { return serverEncryptionAlgorithm; }

  public void setServerEncryptionAlgorithm(FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm) {
    this.serverEncryptionAlgorithm = serverEncryptionAlgorithm;
  }

  public String getServerEncryptionKey() { return serverEncryptionKey; }

  public void setServerEncryptionKey(String serverEncryptionKey) { this.serverEncryptionKey = serverEncryptionKey; }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeaturestoreS3Connector)) {
      return false;
    }
    
    FeaturestoreS3Connector that = (FeaturestoreS3Connector) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    if (!featurestore.equals(that.featurestore)) {
      return false;
    }
    return bucket.equals(that.bucket);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + bucket.hashCode();
    return result;
  }
}
