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
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Abstract JSON template for the featurestore. Contains the common fields and functionality between feature groups
 * and training dataset JSON requests.
 */
@XmlRootElement
public abstract class FeaturestoreEntityJsonDTO {

  private String jobName;
  private String description;
  private Integer version;
  private String name;
  private FeatureCorrelationMatrixDTO featureCorrelationMatrix;
  private DescriptiveStatsDTO descriptiveStatistics;
  private FeatureDistributionsDTO featuresHistogram;
  private ClusterAnalysisDTO clusterAnalysis;
  private boolean updateMetadata = false;
  private boolean updateStats = false;
  private List<FeatureDTO> features;


  public FeaturestoreEntityJsonDTO(
      String description, Integer version, String name,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
      FeatureDistributionsDTO featuresHistogram, ClusterAnalysisDTO clusterAnalysis,
      boolean updateMetadata, boolean updateStats, List<FeatureDTO> features, String jobName) {
    this.description = description;
    this.version = version;
    this.name = name;
    this.featureCorrelationMatrix = featureCorrelationMatrix;
    this.descriptiveStatistics = descriptiveStatistics;
    this.featuresHistogram = featuresHistogram;
    this.clusterAnalysis = clusterAnalysis;
    this.updateMetadata = updateMetadata;
    this.updateStats = updateStats;
    this.features = features;
    this.jobName = jobName;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  @XmlElement
  public Integer getVersion() {
    return version;
  }

  @XmlElement
  public String getName() {
    return name;
  }

  @XmlElement
  public FeatureCorrelationMatrixDTO getFeatureCorrelationMatrix() {
    return featureCorrelationMatrix;
  }

  @XmlElement
  public DescriptiveStatsDTO getDescriptiveStatistics() {
    return descriptiveStatistics;
  }

  @XmlElement
  public FeatureDistributionsDTO getFeaturesHistogram() {
    return featuresHistogram;
  }

  @XmlElement
  public ClusterAnalysisDTO getClusterAnalysis() {
    return clusterAnalysis;
  }

  @XmlElement
  public boolean isUpdateMetadata() {
    return updateMetadata;
  }

  @XmlElement
  public boolean isUpdateStats() {
    return updateStats;
  }

  @XmlElement
  public List<FeatureDTO> getFeatures() {
    return features;
  }

  @XmlElement
  public String getJobName() {
    return jobName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setFeatureCorrelationMatrix(FeatureCorrelationMatrixDTO featureCorrelationMatrix) {
    this.featureCorrelationMatrix = featureCorrelationMatrix;
  }

  public void setDescriptiveStatistics(DescriptiveStatsDTO descriptiveStatistics) {
    this.descriptiveStatistics = descriptiveStatistics;
  }

  public void setFeaturesHistogram(FeatureDistributionsDTO featuresHistogram) {
    this.featuresHistogram = featuresHistogram;
  }

  public void setClusterAnalysis(ClusterAnalysisDTO clusterAnalysis) {
    this.clusterAnalysis = clusterAnalysis;
  }

  public void setUpdateMetadata(boolean updateMetadata) {
    this.updateMetadata = updateMetadata;
  }

  public void setUpdateStats(boolean updateStats) {
    this.updateStats = updateStats;
  }

  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  @Override
  public String toString() {
    return "FeaturestoreEntityJsonDTO{" +
        "jobName=" + jobName +
        ", description='" + description + '\'' +
        ", version=" + version +
        ", name='" + name + '\'' +
        ", featureCorrelationMatrix=" + featureCorrelationMatrix +
        ", descriptiveStatistics=" + descriptiveStatistics +
        ", featuresHistogram=" + featuresHistogram +
        ", clusterAnalysis=" + clusterAnalysis +
        ", updateMetadata=" + updateMetadata +
        ", updateStats=" + updateStats +
        ", features=" + features +
        ", name=" + name +
        '}';
  }
}
