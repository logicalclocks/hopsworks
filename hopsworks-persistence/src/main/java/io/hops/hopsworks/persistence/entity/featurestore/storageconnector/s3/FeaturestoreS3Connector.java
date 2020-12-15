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

import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the feature_store_s3_connector table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_s3_connector", catalog = "hopsworks")
@XmlRootElement
public class FeaturestoreS3Connector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Column(name = "bucket")
  private String bucket;
  @Size(max = 2048)
  @Column(name = "iam_role")
  private String iamRole;
  @Column(name = "server_encryption_algorithm")
  @Enumerated(EnumType.ORDINAL)
  private FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm;
  @Column(name = "server_encryption_key")
  private String serverEncryptionKey;
  @JoinColumns({@JoinColumn(name = "key_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "key_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret secret;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getBucket() {
    return bucket;
  }
  
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }
  
  public String getIamRole() {
    return iamRole;
  }

  public void setIamRole(String iamRole) {
    this.iamRole = iamRole;
  }

  public FeaturestoreS3ConnectorEncryptionAlgorithm getServerEncryptionAlgorithm() { return serverEncryptionAlgorithm; }

  public void setServerEncryptionAlgorithm(FeaturestoreS3ConnectorEncryptionAlgorithm serverEncryptionAlgorithm) {
    this.serverEncryptionAlgorithm = serverEncryptionAlgorithm;
  }

  public String getServerEncryptionKey() { return serverEncryptionKey; }

  public void setServerEncryptionKey(String serverEncryptionKey) { this.serverEncryptionKey = serverEncryptionKey; }

  public Secret getSecret() {
    return secret;
  }

  public void setSecret(Secret secret) {
    this.secret = secret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FeaturestoreS3Connector that = (FeaturestoreS3Connector) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
