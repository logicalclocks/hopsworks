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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of an on-demand featuregroup in the feature store, can be
 * converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
public class OnDemandFeaturegroupDTO extends FeaturegroupDTO {
  
  private Integer jdbcConnectorId;
  private String jdbcConnectorName;
  private String query;
  
  public OnDemandFeaturegroupDTO() {
    super();
  }
  
  public OnDemandFeaturegroupDTO(Featuregroup featuregroup) {
    super(featuregroup);
    if (featuregroup.getOnDemandFeaturegroup().getFeaturestoreJdbcConnector() != null) {
      FeaturestoreJdbcConnector featurestoreJdbcConnector =
        featuregroup.getOnDemandFeaturegroup().getFeaturestoreJdbcConnector();
      this.jdbcConnectorId = featurestoreJdbcConnector.getId();
      this.jdbcConnectorName = featurestoreJdbcConnector.getName();
    }
    setName(featuregroup.getOnDemandFeaturegroup().getName());
    setDescription(featuregroup.getOnDemandFeaturegroup().getDescription());
    this.query = featuregroup.getOnDemandFeaturegroup().getQuery();
    setFeatures(featuregroup.getOnDemandFeaturegroup().getFeatures().stream().map(fgFeature ->
        new FeatureDTO(fgFeature.getName(), fgFeature.getType(), fgFeature.getDescription(),
            fgFeature.getPrimary() == 1,
            false)).collect(Collectors.toList()));
  }
  
  @XmlElement
  public Integer getJdbcConnectorId() {
    return jdbcConnectorId;
  }
  
  public void setJdbcConnectorId(Integer jdbcConnectorId) {
    this.jdbcConnectorId = jdbcConnectorId;
  }
  
  @XmlElement
  public String getQuery() {
    return query;
  }
  
  public void setQuery(String query) {
    this.query = query;
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
    return "HopsfsTrainingDatasetDTO{" +
        "jdbcConnectorId=" + jdbcConnectorId +
        ", jdbcConnectorName='" + jdbcConnectorName + '\'' +
        ", query='" + query + '\'' +
        '}';
  }
}
