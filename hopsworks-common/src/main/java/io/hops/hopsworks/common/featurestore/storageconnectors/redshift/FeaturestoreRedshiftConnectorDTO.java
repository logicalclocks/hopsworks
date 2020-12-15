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
package io.hops.hopsworks.common.featurestore.storageconnectors.redshift;

import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import org.apache.parquet.Strings;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;

@XmlRootElement
public class FeaturestoreRedshiftConnectorDTO extends FeaturestoreStorageConnectorDTO {
  private String clusterIdentifier;
  private String databaseDriver;
  private String databaseEndpoint;
  private String databaseName;
  private Integer databasePort;
  private String tableName;
  private String databaseUserName;
  private Boolean autoCreate;
  private String databasePassword;
  private String databaseGroup;
  private String iamRole;
  private String arguments;

  private Instant expiration;

  public FeaturestoreRedshiftConnectorDTO() {
  }

  public FeaturestoreRedshiftConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
    this.clusterIdentifier = featurestoreConnector.getRedshiftConnector().getClusterIdentifier();
    this.databaseDriver = featurestoreConnector.getRedshiftConnector().getDatabaseDriver();
    this.databaseEndpoint = featurestoreConnector.getRedshiftConnector().getDatabaseEndpoint();
    this.databaseName = featurestoreConnector.getRedshiftConnector().getDatabaseName();
    this.databasePort = featurestoreConnector.getRedshiftConnector().getDatabasePort();
    this.tableName = featurestoreConnector.getRedshiftConnector().getTableName();
    this.databaseUserName = featurestoreConnector.getRedshiftConnector().getDatabaseUserName();
    this.autoCreate = featurestoreConnector.getRedshiftConnector().getAutoCreate();
    this.databaseGroup = featurestoreConnector.getRedshiftConnector().getDatabaseGroup();
    this.iamRole = featurestoreConnector.getRedshiftConnector().getIamRole();
    this.arguments = featurestoreConnector.getRedshiftConnector().getArguments();
  }

  @XmlElement
  public String getClusterIdentifier() {
    return clusterIdentifier;
  }

  public void setClusterIdentifier(String clusterIdentifier) {
    this.clusterIdentifier = clusterIdentifier;
  }

  @XmlElement
  public String getDatabaseDriver() {
    return databaseDriver;
  }

  public void setDatabaseDriver(String databaseDriver) {
    this.databaseDriver = databaseDriver;
  }

  @XmlElement
  public String getDatabaseEndpoint() {
    return databaseEndpoint;
  }

  public void setDatabaseEndpoint(String databaseEndpoint) {
    this.databaseEndpoint = databaseEndpoint;
  }

  @XmlElement
  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  @XmlElement
  public Integer getDatabasePort() {
    return databasePort;
  }

  public void setDatabasePort(Integer databasePort) {
    this.databasePort = databasePort;
  }

  @XmlElement
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @XmlElement
  public String getDatabaseUserName() {
    return databaseUserName;
  }

  public void setDatabaseUserName(String databaseUserName) {
    this.databaseUserName = databaseUserName;
  }

  @XmlElement
  public Boolean getAutoCreate() {
    return autoCreate;
  }

  public void setAutoCreate(Boolean autoCreate) {
    this.autoCreate = autoCreate;
  }

  @XmlElement
  public String getDatabaseGroup() {
    return databaseGroup;
  }

  public void setDatabaseGroup(String databaseGroup) {
    this.databaseGroup = databaseGroup;
  }

  @XmlElement
  public Instant getExpiration() {
    return expiration;
  }

  public void setExpiration(Instant expiration) {
    this.expiration = expiration;
  }

  public String[] getDatabaseGroups() {
    if (!Strings.isNullOrEmpty(this.databaseGroup)) {
      return this.databaseGroup.split(":");
    } else {
      return new String[]{};
    }
  }

  @XmlElement
  public String getDatabasePassword() {
    return databasePassword;
  }

  public void setDatabasePassword(String databasePassword) {
    this.databasePassword = databasePassword;
  }

  @XmlElement
  public String getIamRole() {
    return iamRole;
  }

  public void setIamRole(String iamRole) {
    this.iamRole = iamRole;
  }

  @XmlElement
  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  @Override
  public String toString() {
    return "FeaturestoreRedshiftConnectorDTO{clusterIdentifier='" + clusterIdentifier + '}';
  }
}
