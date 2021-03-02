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
package io.hops.hopsworks.common.featurestore.storageconnectors.snowflake;

import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class FeaturestoreSnowflakeConnectorDTO extends FeaturestoreStorageConnectorDTO {
  private String url;
  private String user;
  private String password;
  private String token;
  private String database;
  private String schema;
  private String warehouse;
  private String role;
  private String table;
  private List<OptionDTO> sfOptions;
  
  public FeaturestoreSnowflakeConnectorDTO() {
  }
  
  public FeaturestoreSnowflakeConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
    this.url = featurestoreConnector.getSnowflakeConnector().getUrl();
    this.user = featurestoreConnector.getSnowflakeConnector().getDatabaseUser();
    this.database = featurestoreConnector.getSnowflakeConnector().getDatabaseName();
    this.schema = featurestoreConnector.getSnowflakeConnector().getDatabaseSchema();
    this.warehouse = featurestoreConnector.getSnowflakeConnector().getWarehouse();
    this.role = featurestoreConnector.getSnowflakeConnector().getRole();
    this.table = featurestoreConnector.getSnowflakeConnector().getTableName();
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getToken() {
    return token;
  }
  
  public void setToken(String token) {
    this.token = token;
  }
  
  public String getDatabase() {
    return database;
  }
  
  public void setDatabase(String database) {
    this.database = database;
  }
  
  public String getSchema() {
    return schema;
  }
  
  public void setSchema(String schema) {
    this.schema = schema;
  }
  
  public String getWarehouse() {
    return warehouse;
  }
  
  public void setWarehouse(String warehouse) {
    this.warehouse = warehouse;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public String getTable() {
    return table;
  }
  
  public void setTable(String table) {
    this.table = table;
  }
  
  public List<OptionDTO> getSfOptions() {
    return sfOptions;
  }
  
  public void setSfOptions(List<OptionDTO> sfOptions) {
    this.sfOptions = sfOptions;
  }
}
