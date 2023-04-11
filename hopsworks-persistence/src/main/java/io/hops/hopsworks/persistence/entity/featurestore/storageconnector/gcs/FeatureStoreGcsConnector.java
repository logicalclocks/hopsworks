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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.gcs;

import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "feature_store_gcs_connector", catalog = "hopsworks")
@XmlRootElement
public class FeatureStoreGcsConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "key_path")
  private String keyPath;

  @JoinColumns({@JoinColumn(name = "encryption_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "encryption_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret encryptionSecret;
  
  @Column(name = "algorithm")
  private EncryptionAlgorithm algorithm;
  
  @Column(name = "bucket", nullable = false, length = 1000)
  private String bucket;
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Secret getEncryptionSecret() {
    return encryptionSecret;
  }

  public void setEncryptionSecret(Secret encryptionSecret) {
    this.encryptionSecret = encryptionSecret;
  }

  public String getKeyPath() {
    return keyPath;
  }

  public void setKeyPath(String keyPath) {
    this.keyPath = keyPath;
  }

  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public EncryptionAlgorithm getAlgorithm() {
    return algorithm;
  }
  
  public void setAlgorithm(EncryptionAlgorithm algorithm) {
    this.algorithm = algorithm;
  }
  
  public String getBucket() {
    return bucket;
  }
  
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FeatureStoreGcsConnector that = (FeatureStoreGcsConnector) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(keyPath, that.keyPath)) return false;
    if (!Objects.equals(encryptionSecret, that.encryptionSecret))
      return false;
    if (algorithm != that.algorithm) return false;
    return Objects.equals(bucket, that.bucket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, keyPath, encryptionSecret, algorithm);
  }
}
