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

package io.hops.hopsworks.persistence.entity.featurestore.storageconnector;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.adls.FeaturestoreADLSConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.redshift.FeatureStoreRedshiftConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake.FeaturestoreSnowflakeConnector;

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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing a feature_store_connector table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_connector", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreConnector.findAll",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn"),
    @NamedQuery(name = "FeaturestoreConnector.findById",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn WHERE fsConn.id = :id"),
    @NamedQuery(name = "FeaturestoreConnector.findByIdType",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn " +
            "WHERE fsConn.id = :id AND fsConn.connectorType= :type"),
    @NamedQuery(name = "FeaturestoreConnector.findByFeaturestore",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn WHERE fsConn.featurestore = :featurestore"),
    @NamedQuery(name = "FeaturestoreConnector.countByFeaturestore",
        query = "SELECT count(fsConn.id) FROM FeaturestoreConnector fsConn WHERE fsConn.featurestore = :featurestore"),
    @NamedQuery(name = "FeaturestoreConnector.findByFeaturestoreId",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn " +
            "WHERE fsConn.featurestore = :featurestore AND fsConn.id = :id"),
    @NamedQuery(name = "FeaturestoreConnector.findByFeaturestoreName",
        query = "SELECT fsConn FROM FeaturestoreConnector fsConn " +
            "WHERE fsConn.featurestore = :featurestore AND fsConn.name = :name")})
public class FeaturestoreConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  private Featurestore featurestore;
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @Column(name = "type")
  @Enumerated(EnumType.ORDINAL)
  private FeaturestoreConnectorType connectorType;

  @JoinColumn(name = "jdbc_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeaturestoreJdbcConnector jdbcConnector;
  @JoinColumn(name = "s3_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeaturestoreS3Connector s3Connector;
  @JoinColumn(name = "hopsfs_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeaturestoreHopsfsConnector hopsfsConnector;
  @JoinColumn(name = "redshift_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeatureStoreRedshiftConnector redshiftConnector;
  @JoinColumn(name = "adls_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeaturestoreADLSConnector adlsConnector;
  @JoinColumn(name = "snowflake_id", referencedColumnName = "id")
  @ManyToOne(cascade = CascadeType.ALL)
  private FeaturestoreSnowflakeConnector snowflakeConnector;

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

  public FeaturestoreConnectorType getConnectorType() {
    return connectorType;
  }

  public void setConnectorType(FeaturestoreConnectorType connectorType) {
    this.connectorType = connectorType;
  }

  public FeaturestoreJdbcConnector getJdbcConnector() {
    return jdbcConnector;
  }

  public void setJdbcConnector(FeaturestoreJdbcConnector jdbcConnector) {
    this.jdbcConnector = jdbcConnector;
  }

  public FeaturestoreS3Connector getS3Connector() {
    return s3Connector;
  }

  public void setS3Connector(FeaturestoreS3Connector s3Connector) {
    this.s3Connector = s3Connector;
  }

  public FeaturestoreHopsfsConnector getHopsfsConnector() {
    return hopsfsConnector;
  }

  public void setHopsfsConnector(FeaturestoreHopsfsConnector hopsfsConnector) {
    this.hopsfsConnector = hopsfsConnector;
  }

  public FeatureStoreRedshiftConnector getRedshiftConnector() {
    return redshiftConnector;
  }

  public void setRedshiftConnector(FeatureStoreRedshiftConnector redshiftConnector) {
    this.redshiftConnector = redshiftConnector;
  }
  public FeaturestoreADLSConnector getAdlsConnector() {
    return adlsConnector;
  }

  public void setAdlsConnector(FeaturestoreADLSConnector adlsConnector) {
    this.adlsConnector = adlsConnector;
  }
  
  public FeaturestoreSnowflakeConnector getSnowflakeConnector() {
    return snowflakeConnector;
  }
  
  public void setSnowflakeConnector(FeaturestoreSnowflakeConnector snowflakeConnector) {
    this.snowflakeConnector = snowflakeConnector;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FeaturestoreConnector that = (FeaturestoreConnector) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
