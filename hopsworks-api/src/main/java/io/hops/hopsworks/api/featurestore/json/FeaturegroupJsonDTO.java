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

package io.hops.hopsworks.api.featurestore.json;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO representing the JSON payload from REST-clients when creating/updating featuregroups
 */
@XmlRootElement
public class FeaturegroupJsonDTO extends FeaturestoreEntityJsonDTO {

  private FeaturegroupType featuregroupType = FeaturegroupType.CACHED_FEATURE_GROUP;
  private Integer jdbcConnectorId;
  private String sqlQuery;
  
  public FeaturegroupJsonDTO() {
    super(null, null, null,
        null, null, null, null,
        false, false, null, null);
  }

  public FeaturegroupJsonDTO(
      List<FeatureDTO> features, String featuregroupName,
      String description, Integer version,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
      boolean updateMetadata,
      boolean updateStats, FeatureDistributionsDTO featuresHistogram, ClusterAnalysisDTO clusterAnalysis,
      String jobName, FeaturegroupType featuregroupType, Integer jdbcConnectorId, String sqlQuery) {
    super(description, version, featuregroupName, featureCorrelationMatrix, descriptiveStatistics,
        featuresHistogram, clusterAnalysis, updateMetadata, updateStats, features, jobName);
    this.featuregroupType = featuregroupType;
    this.jdbcConnectorId = jdbcConnectorId;
    this.sqlQuery = sqlQuery;
  }
  
  @XmlElement
  public FeaturegroupType getFeaturegroupType() {
    return featuregroupType;
  }
  
  public void setFeaturegroupType(FeaturegroupType featuregroupType) {
    this.featuregroupType = featuregroupType;
  }
  
  @XmlElement
  public Integer getJdbcConnectorId() {
    return jdbcConnectorId;
  }
  
  public void setJdbcConnectorId(Integer jdbcConnectorId) {
    this.jdbcConnectorId = jdbcConnectorId;
  }
  
  @XmlElement
  public String getSqlQuery() {
    return sqlQuery;
  }
  
  public void setSqlQuery(String sqlQuery) {
    this.sqlQuery = sqlQuery;
  }
  
  @Override
  public String toString() {
    return "FeaturegroupJsonDTO{" +
      "featuregroupType=" + featuregroupType +
      ", jdbcConnectorId=" + jdbcConnectorId +
      ", sqlQuery='" + sqlQuery + '\'' +
      '}';
  }
}
