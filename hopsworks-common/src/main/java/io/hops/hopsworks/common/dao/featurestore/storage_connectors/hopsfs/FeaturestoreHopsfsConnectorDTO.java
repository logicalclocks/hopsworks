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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a JDBC connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"featurestoreId", "name", "description", "hopsfsPath", "id"})
public class FeaturestoreHopsfsConnectorDTO {
  
  private Integer featurestoreId;
  private String name;
  private String description;
  private String hopsfsPath;
  private Integer id;
  
  public FeaturestoreHopsfsConnectorDTO() {
  }
  
  public FeaturestoreHopsfsConnectorDTO(FeaturestoreHopsfsConnector featurestoreHopsfsConnector) {
    this.hopsfsPath = null;
    this.featurestoreId = featurestoreHopsfsConnector.getFeaturestore().getId();
    this.name = featurestoreHopsfsConnector.getName();
    this.description = featurestoreHopsfsConnector.getDescription();
    this.id = featurestoreHopsfsConnector.getId();
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
  public String getHopsfsPath() {
    return hopsfsPath;
  }
  
  public void setHopsfsPath(String hopsfsPath) {
    this.hopsfsPath = hopsfsPath;
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
    return "FeaturestoreHopsfsConnectorDTO{" +
      "featurestoreId=" + featurestoreId +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", hopsfsPath='" + hopsfsPath + '\'' +
      ", id=" + id +
      '}';
  }
}
