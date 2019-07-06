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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a JDBC connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
public class FeaturestoreJdbcConnectorDTO extends FeaturestoreStorageConnectorDTO {

  private String connectionString;
  private String arguments;

  public FeaturestoreJdbcConnectorDTO() {
    super(null, null, null, null, null);
  }

  public FeaturestoreJdbcConnectorDTO(FeaturestoreJdbcConnector featurestoreJdbcConnector) {
    super(featurestoreJdbcConnector.getId(), featurestoreJdbcConnector.getDescription(),
        featurestoreJdbcConnector.getName(), featurestoreJdbcConnector.getFeaturestore().getId(),
        FeaturestoreStorageConnectorType.JDBC);
    this.connectionString = featurestoreJdbcConnector.getConnectionString();
    this.arguments = featurestoreJdbcConnector.getArguments();
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

  @Override
  public String toString() {
    return "FeaturestoreJdbcConnectorDTO{" +
        "connectionString='" + connectionString + '\'' +
        ", arguments='" + arguments + '\'' +
        '}';
  }
}
