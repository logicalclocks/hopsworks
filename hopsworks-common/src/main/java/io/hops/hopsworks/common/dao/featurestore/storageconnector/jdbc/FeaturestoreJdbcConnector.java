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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entity class representing the feature_store_jdbc_connector table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_jdbc_connector", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreJdbcConnector.findAll", query = "SELECT fsjdbc FROM FeaturestoreJdbcConnector " +
      "fsjdbc"),
    @NamedQuery(name = "FeaturestoreJdbcConnector.findById",
        query = "SELECT fsjdbc FROM FeaturestoreJdbcConnector fsjdbc WHERE fsjdbc.id = :id"),
    @NamedQuery(name = "FeaturestoreJdbcConnector.findByFeaturestore", query = "SELECT fsjdbc " +
        "FROM FeaturestoreJdbcConnector fsjdbc WHERE fsjdbc.featurestore = :featurestore"),
    @NamedQuery(name = "FeaturestoreJdbcConnector.findByFeaturestoreAndId", query = "SELECT fsjdbc " +
        "FROM FeaturestoreJdbcConnector fsjdbc WHERE fsjdbc.featurestore = :featurestore AND fsjdbc.id = :id")})
public class FeaturestoreJdbcConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  private Featurestore featurestore;
  @Basic(optional = false)
  @Column(name = "connection_string")
  private String connectionString;
  @Column(name = "arguments")
  private String arguments;
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;

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
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeaturestoreJdbcConnector)) return false;
    
    FeaturestoreJdbcConnector that = (FeaturestoreJdbcConnector) o;
    
    if (!id.equals(that.id)) return false;
    if (!featurestore.equals(that.featurestore)) return false;
    if (!connectionString.equals(that.connectionString)) return false;
    return connectionString.equals(that.connectionString);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + connectionString.hashCode();
    return result;
  }
}
