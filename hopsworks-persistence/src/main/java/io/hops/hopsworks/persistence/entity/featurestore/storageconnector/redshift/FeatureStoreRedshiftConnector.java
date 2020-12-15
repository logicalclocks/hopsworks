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
package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.redshift;

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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "feature_store_redshift_connector", catalog = "hopsworks")
@XmlRootElement
public class FeatureStoreRedshiftConnector implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 64)
  @Column(name = "cluster_identifier")
  private String clusterIdentifier;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 64)
  @Column(name = "database_driver")
  private String databaseDriver;
  @Size(min = 1, max = 128)
  @Column(name = "database_endpoint")
  private String databaseEndpoint;
  @Size(min = 1, max = 64)
  @Column(name = "database_name")
  private String databaseName;
  @Column(name = "database_port")
  private int databasePort;
  @Size(max = 128)
  @Column(name = "table_name")
  private String tableName;
  @Size(max = 128)
  @Column(name = "database_user_name")
  private String databaseUserName;
  @Column(name = "auto_create")
  private Boolean autoCreate;
  @Size(max = 2048)
  @Column(name = "database_group")
  private String databaseGroup;
  @Size(max = 2048)
  @Column(name = "iam_role")
  private String iamRole;
  @Size(max = 2000)
  @Column(name = "arguments")
  private String arguments;
  @JoinColumns({@JoinColumn(name = "database_pwd_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "database_pwd_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret secret;

  public FeatureStoreRedshiftConnector() {
  }

  public FeatureStoreRedshiftConnector(Integer id) {
    this.id = id;
  }

  public FeatureStoreRedshiftConnector(Integer id, String clusterIdentifier, String databaseEndpoint,
    String databaseName, int databasePort) {
    this.id = id;
    this.clusterIdentifier = clusterIdentifier;
    this.databaseEndpoint = databaseEndpoint;
    this.databaseName = databaseName;
    this.databasePort = databasePort;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getClusterIdentifier() {
    return clusterIdentifier;
  }

  public void setClusterIdentifier(String clusterIdentifier) {
    this.clusterIdentifier = clusterIdentifier;
  }

  public String getDatabaseDriver() {
    return databaseDriver;
  }

  public void setDatabaseDriver(String databaseDriver) {
    this.databaseDriver = databaseDriver;
  }

  public String getDatabaseEndpoint() {
    return databaseEndpoint;
  }

  public void setDatabaseEndpoint(String databaseEndpoint) {
    this.databaseEndpoint = databaseEndpoint;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public int getDatabasePort() {
    return databasePort;
  }

  public void setDatabasePort(int databasePort) {
    this.databasePort = databasePort;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getDatabaseUserName() {
    return databaseUserName;
  }

  public void setDatabaseUserName(String databaseUserName) {
    this.databaseUserName = databaseUserName;
  }

  public Boolean getAutoCreate() {
    return autoCreate;
  }

  public void setAutoCreate(Boolean autoCreate) {
    this.autoCreate = autoCreate;
  }

  public String getDatabaseGroup() {
    return databaseGroup;
  }

  public void setDatabaseGroup(String databaseGroup) {
    this.databaseGroup = databaseGroup;
  }

  public Secret getSecret() {
    return secret;
  }

  public void setSecret(Secret secret) {
    this.secret = secret;
  }

  public String getIamRole() {
    return iamRole;
  }

  public void setIamRole(String iamRole) {
    this.iamRole = iamRole;
  }

  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof FeatureStoreRedshiftConnector)) {
      return false;
    }
    FeatureStoreRedshiftConnector other = (FeatureStoreRedshiftConnector) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }
}
