/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.snowflake;

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
@Table(name = "feature_store_snowflake_connector", catalog = "hopsworks")
@XmlRootElement
public class FeaturestoreSnowflakeConnector implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 3000)
  @Column(name = "url")
  private String url;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "database_user")
  private String databaseUser;
  @NotNull
  @Size(min = 1, max = 64)
  @Column(name = "database_name")
  private String databaseName;
  @NotNull
  @Size(min = 1, max = 45)
  @Column(name = "database_schema")
  private String databaseSchema;
  @Size(max = 128)
  @Column(name = "table_name")
  private String tableName;
  @Size(max = 65)
  @Column(name = "role")
  private String role;
  @Size(max = 128)
  @Column(name = "warehouse")
  private String warehouse;
  @Size(max = 8000)
  @Column(name = "arguments")
  private String arguments;
  @JoinColumns({@JoinColumn(name = "database_pwd_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "database_pwd_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret pwdSecret;
  @JoinColumns({@JoinColumn(name = "oauth_token_secret_uid", referencedColumnName = "uid"),
    @JoinColumn(name = "oauth_token_secret_name", referencedColumnName = "secret_name")})
  @ManyToOne(cascade = CascadeType.ALL)
  private Secret tokenSecret;

  public FeaturestoreSnowflakeConnector() {
  }

  public FeaturestoreSnowflakeConnector(Integer id) {
    this.id = id;
  }
  
  public FeaturestoreSnowflakeConnector(Integer id, String url, String databaseUser, String databaseName,
    String databaseSchema, String tableName, String role, String warehouse, String arguments) {
    this.id = id;
    this.url = url;
    this.databaseUser = databaseUser;
    this.databaseName = databaseName;
    this.databaseSchema = databaseSchema;
    this.tableName = tableName;
    this.role = role;
    this.warehouse = warehouse;
    this.arguments = arguments;
  }
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getDatabaseUser() {
    return databaseUser;
  }
  
  public void setDatabaseUser(String databaseUser) {
    this.databaseUser = databaseUser;
  }
  
  public String getDatabaseName() {
    return databaseName;
  }
  
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }
  
  public String getDatabaseSchema() {
    return databaseSchema;
  }
  
  public void setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
  }
  
  public String getTableName() {
    return tableName;
  }
  
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public String getWarehouse() {
    return warehouse;
  }
  
  public void setWarehouse(String warehouse) {
    this.warehouse = warehouse;
  }
  
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
  
  public Secret getPwdSecret() {
    return pwdSecret;
  }
  
  public void setPwdSecret(Secret pwdSecret) {
    this.pwdSecret = pwdSecret;
  }
  
  public Secret getTokenSecret() {
    return tokenSecret;
  }
  
  public void setTokenSecret(Secret tokenSecret) {
    this.tokenSecret = tokenSecret;
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
    if (!(object instanceof FeaturestoreSnowflakeConnector)) {
      return false;
    }
    FeaturestoreSnowflakeConnector other = (FeaturestoreSnowflakeConnector) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }
}
