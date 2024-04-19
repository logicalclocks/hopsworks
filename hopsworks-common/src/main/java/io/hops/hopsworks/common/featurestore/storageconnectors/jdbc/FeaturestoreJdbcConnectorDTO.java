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

package io.hops.hopsworks.common.featurestore.storageconnectors.jdbc;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO containing the human-readable information of a JDBC connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
@JsonTypeInfo(
  use=JsonTypeInfo.Id.NAME,
  include=JsonTypeInfo.As.PROPERTY,
  property="type")
@JsonTypeName("featurestoreJdbcConnectorDTO")
public class FeaturestoreJdbcConnectorDTO extends FeaturestoreStorageConnectorDTO {

  private String connectionString;
  private List<OptionDTO> arguments;

  private String driverPath;
  
  public FeaturestoreJdbcConnectorDTO() {
  }

  public FeaturestoreJdbcConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
    this.connectionString = featurestoreConnector.getJdbcConnector().getConnectionString();
  }

  @XmlElement
  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  @XmlElement
  public List<OptionDTO> getArguments() {
    return arguments;
  }
  
  public void setArguments(List<OptionDTO> arguments) {
    this.arguments = arguments;
  }

  @XmlElement
  public String getDriverPath() {return driverPath; };
  
  public void setDriverPath(String driverPath) {
    this.driverPath = driverPath;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreJdbcConnectorDTO{" +
      "connectionString='" + connectionString + '\'' +
      ", arguments=" + arguments +
      ", driverPath='" + driverPath + '\'' +
      '}';
  }
}
