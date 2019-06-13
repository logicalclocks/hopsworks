/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a JDBC connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"connectionString", "arguments", "featurestoreId", "name", "description", "id"})
public class FeaturestoreJdbcConnectorDTO {

  private String connectionString;
  private String arguments;
  private Integer featurestoreId;
  private String name;
  private String description;
  private Integer id;
  
  public FeaturestoreJdbcConnectorDTO() {
  }
  
  public FeaturestoreJdbcConnectorDTO(FeaturestoreJdbcConnector featurestoreJdbcConnector) {
    this.connectionString = featurestoreJdbcConnector.getConnectionString();
    this.arguments = featurestoreJdbcConnector.getArguments();
    this.featurestoreId = featurestoreJdbcConnector.getFeaturestore().getId();
    this.name = featurestoreJdbcConnector.getName();
    this.description = featurestoreJdbcConnector.getDescription();
    this.id = featurestoreJdbcConnector.getId();
  }
  
  @XmlElement
  public String getConnectionString() {
    return connectionString;
  }
  
  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }
  
  @XmlElement
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
  
  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
  
  @XmlElement
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @XmlElement
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  @XmlElement
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreExternalSQLQueryDTO{" +
      "connectionString='" + connectionString + '\'' +
      ", arguments='" + arguments + '\'' +
      ", featurestoreId=" + featurestoreId +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", id=" + id +
      '}';
  }
}
