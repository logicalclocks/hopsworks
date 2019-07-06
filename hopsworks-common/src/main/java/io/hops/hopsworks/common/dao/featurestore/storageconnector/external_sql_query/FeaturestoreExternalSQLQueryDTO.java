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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.external_sql_query;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of an external SQL query for a feature store, can be converted to
 * JSON or XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"jdbcConnectorId", "jdbcConnectorName", "name", "description", "query", "id", "features"})
public class FeaturestoreExternalSQLQueryDTO {
  
  private Integer jdbcConnectorId;
  private String jdbcConnectorName;
  private String name;
  private String description;
  private String query;
  private Integer id;
  private List<FeatureDTO> features;
  
  public FeaturestoreExternalSQLQueryDTO() {
  }
  
  public FeaturestoreExternalSQLQueryDTO(FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery) {
    if (featurestoreExternalSQLQuery.getFeaturestoreJdbcConnector() != null) {
      FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreExternalSQLQuery.getFeaturestoreJdbcConnector();
      this.jdbcConnectorId = featurestoreJdbcConnector.getId();
      this.jdbcConnectorName = featurestoreJdbcConnector.getName();
    }
    this.name = featurestoreExternalSQLQuery.getName();
    this.description = featurestoreExternalSQLQuery.getDescription();
    this.query = featurestoreExternalSQLQuery.getQuery();
    this.id = featurestoreExternalSQLQuery.getId();
    this.features = featurestoreExternalSQLQuery.getFeatures().stream().map(fgFeature ->
      new FeatureDTO(fgFeature.getName(), fgFeature.getType(), fgFeature.getDescription(),
        new Boolean(fgFeature.getPrimary() == 1),
        false))
      .collect(Collectors.toList());
  }
  
  @XmlElement
  public Integer getJdbcConnectorId() {
    return jdbcConnectorId;
  }
  
  public void setJdbcConnectorId(Integer jdbcConnectorId) {
    this.jdbcConnectorId = jdbcConnectorId;
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
  public String getQuery() {
    return query;
  }
  
  public void setQuery(String query) {
    this.query = query;
  }
  
  @XmlElement
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @XmlElement
  public List<FeatureDTO> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }
  
  @XmlElement
  public String getJdbcConnectorName() {
    return jdbcConnectorName;
  }
  
  public void setJdbcConnectorName(String jdbcConnectorName) {
    this.jdbcConnectorName = jdbcConnectorName;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreExternalSQLQueryDTO{" +
      "jdbcConnectorId=" + jdbcConnectorId +
      ", jdbcConnectorName='" + jdbcConnectorName + '\'' +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", query='" + query + '\'' +
      ", id=" + id +
      ", features=" + features +
      '}';
  }
}
