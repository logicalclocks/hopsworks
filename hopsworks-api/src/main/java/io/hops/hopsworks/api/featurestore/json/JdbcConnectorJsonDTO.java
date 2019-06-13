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

package io.hops.hopsworks.api.featurestore.json;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO representing the JSON payload from REST-clients when creating/updating JDBC storage backends in the featurestore
 */
@XmlRootElement
public class JdbcConnectorJsonDTO extends StorageConnectorJsonDTO {
  
  private List<String> jdbcArguments;
  private String jdbcConnectionString;

  public JdbcConnectorJsonDTO() {
  }
  
  public JdbcConnectorJsonDTO(List<String> jdbcArguments, String jdbcConnectionString) {
    this.jdbcArguments = jdbcArguments;
    this.jdbcConnectionString = jdbcConnectionString;
  }
  
  public JdbcConnectorJsonDTO(String name, String description, List<String> jdbcArguments,
    String jdbcConnectionString) {
    super(name, description);
    this.jdbcArguments = jdbcArguments;
    this.jdbcConnectionString = jdbcConnectionString;
  }
  
  @XmlElement
  public List<String> getJdbcArguments() {
    return jdbcArguments;
  }
  
  public void setJdbcArguments(List<String> jdbcArguments) {
    this.jdbcArguments = jdbcArguments;
  }
  
  @XmlElement
  public String getJdbcConnectionString() {
    return jdbcConnectionString;
  }
  
  public void setJdbcConnectionString(String jdbcConnectionString) {
    this.jdbcConnectionString = jdbcConnectionString;
  }
}
