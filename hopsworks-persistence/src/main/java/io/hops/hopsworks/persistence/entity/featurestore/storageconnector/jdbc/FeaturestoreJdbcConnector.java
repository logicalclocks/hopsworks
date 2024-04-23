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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc;

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
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import java.io.Serializable;

/**
 * Entity class representing the feature_store_jdbc_connector table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_jdbc_connector", catalog = "hopsworks")
@XmlRootElement
public class FeaturestoreJdbcConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "connection_string")
  private String connectionString;
  @Column(name = "arguments")
  private String arguments;
  @JoinColumns({ @JoinColumn(name = "secret_uid", referencedColumnName = "uid"),
      @JoinColumn(name = "secret_name", referencedColumnName = "secret_name") })
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret passwordSecret;

  
  @Column(name = "driver_path")
  private String driverPath;
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getConnectionString() {
    return connectionString;
  }
  
  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }
  
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  public Secret getPasswordSecret() {
    return passwordSecret;
  }

  public void setPasswordSecret(Secret encryptionSecret) {
    this.passwordSecret = encryptionSecret;
  }

  
  public String getDriverPath() {
    return driverPath;
  }
  
  public void setDriverPath(String driverPath) {
    this.driverPath = driverPath;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeaturestoreJdbcConnector)) return false;
    
    FeaturestoreJdbcConnector that = (FeaturestoreJdbcConnector) o;
    
    if (!id.equals(that.id)) return false;
    if (!connectionString.equals(that.connectionString)) return false;
    return connectionString.equals(that.connectionString);
  }
  
  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
