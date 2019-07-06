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

package io.hops.hopsworks.common.dao.featurestore.storageconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3ConnectorDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Abstract storage connector in the featurestore. Contains the common fields and functionality between different
 * types of storage connectors
 */
@XmlRootElement
@XmlSeeAlso({FeaturestoreHopsfsConnectorDTO.class, FeaturestoreJdbcConnectorDTO.class,
    FeaturestoreS3ConnectorDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FeaturestoreHopsfsConnectorDTO.class, name = "FeaturestoreHopsfsConnectorDTO"),
    @JsonSubTypes.Type(value = FeaturestoreJdbcConnectorDTO.class, name = "FeaturestoreJdbcConnectorDTO"),
    @JsonSubTypes.Type(value = FeaturestoreS3ConnectorDTO.class, name = "FeaturestoreS3ConnectorDTO")}
)
public class FeaturestoreStorageConnectorDTO {
  private Integer id;
  private String description;
  private String name;
  private Integer featurestoreId;
  private FeaturestoreStorageConnectorType storageConnectorType;

  public FeaturestoreStorageConnectorDTO() {
  }

  public FeaturestoreStorageConnectorDTO(Integer id, String description, String name, Integer featurestoreId,
                                         FeaturestoreStorageConnectorType featurestoreStorageConnectorType) {
    this.id = id;
    this.description = description;
    this.name = name;
    this.featurestoreId = featurestoreId;
    this.storageConnectorType = featurestoreStorageConnectorType;
  }

  @XmlElement
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElement
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }

  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }

  @XmlElement
  public FeaturestoreStorageConnectorType getStorageConnectorType() {
    return storageConnectorType;
  }

  public void setStorageConnectorType(FeaturestoreStorageConnectorType storageConnectorType) {
    this.storageConnectorType = storageConnectorType;
  }

  @Override
  public String toString() {
    return "FeaturestoreStorageConnectorDTO{" +
        "id=" + id +
        ", description='" + description + '\'' +
        ", name='" + name + '\'' +
        ", featurestoreId=" + featurestoreId +
        ", storageConnectorType=" + storageConnectorType +
        '}';
  }
}
