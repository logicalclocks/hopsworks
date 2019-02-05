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

package io.hops.hopsworks.common.dao.app;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * JSON payload for appservice featurestore endpoints
 */
@XmlRootElement
public class FeaturestoreJsonDTO extends KeystoreDTO {

  String featurestoreName;
  String name;
  Integer version;
  String description;
  String jobName;
  FeatureCorrelationMatrixDTO featureCorrelationMatrix;
  DescriptiveStatsDTO descriptiveStatistics;
  boolean updateMetadata = false;
  boolean updateStats = false;
  FeatureDistributionsDTO featuresHistogram;
  List<FeatureDTO> features;
  String dataFormat;
  ClusterAnalysisDTO clusterAnalysis;
  List<String> dependencies;


  public FeaturestoreJsonDTO() {
  }

  public FeaturestoreJsonDTO(
      String featurestoreName, String name, Integer version, String description,
      List<String> dependencies, List<FeatureDTO> features,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, boolean updateMetadata, boolean updateStats,
      FeatureDistributionsDTO featuresHistogram,
      String dataFormat, ClusterAnalysisDTO clusterAnalysis, String jobName) {
    this.featurestoreName = featurestoreName;
    this.name = name;
    this.version = version;
    this.description = description;
    this.dependencies = dependencies;
    this.features = features;
    this.featureCorrelationMatrix = featureCorrelationMatrix;
    this.descriptiveStatistics = descriptiveStatistics;
    this.updateMetadata = updateMetadata;
    this.updateStats = updateStats;
    this.featuresHistogram = featuresHistogram;
    this.dataFormat = dataFormat;
    this.clusterAnalysis = clusterAnalysis;
    this.jobName = jobName;
  }

  public String getFeaturestoreName() {
    return featurestoreName;
  }

  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }

  public List<FeatureDTO> getFeatures() {
    return features;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public FeatureCorrelationMatrixDTO getFeatureCorrelationMatrix() {
    return featureCorrelationMatrix;
  }

  public void setFeatureCorrelationMatrix(FeatureCorrelationMatrixDTO featureCorrelationMatrix) {
    this.featureCorrelationMatrix = featureCorrelationMatrix;
  }

  public DescriptiveStatsDTO getDescriptiveStatistics() {
    return descriptiveStatistics;
  }

  public void setDescriptiveStatistics(DescriptiveStatsDTO descriptiveStatistics) {
    this.descriptiveStatistics = descriptiveStatistics;
  }

  public boolean isUpdateMetadata() {
    return updateMetadata;
  }

  public void setUpdateMetadata(boolean updateMetadata) {
    this.updateMetadata = updateMetadata;
  }

  public boolean isUpdateStats() {
    return updateStats;
  }

  public void setUpdateStats(boolean updateStats) {
    this.updateStats = updateStats;
  }

  public FeatureDistributionsDTO getFeaturesHistogram() {
    return featuresHistogram;
  }

  public void setFeaturesHistogram(FeatureDistributionsDTO featuresHistogram) {
    this.featuresHistogram = featuresHistogram;
  }

  public String getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  public ClusterAnalysisDTO getClusterAnalysis() {
    return clusterAnalysis;
  }

  public void setClusterAnalysis(ClusterAnalysisDTO clusterAnalysis) {
    this.clusterAnalysis = clusterAnalysis;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  @Override
  public String toString() {
    return "FeaturestoreJsonDTO{" +
        "featurestoreName='" + featurestoreName + '\'' +
        ", name='" + name + '\'' +
        ", version=" + version +
        ", description='" + description + '\'' +
        ", featureCorrelationMatrix=" + featureCorrelationMatrix +
        ", descriptiveStatistics=" + descriptiveStatistics +
        ", updateMetadata=" + updateMetadata +
        ", updateStats=" + updateStats +
        ", featuresHistogram=" + featuresHistogram +
        ", features=" + features +
        ", dataFormat='" + dataFormat + '\'' +
        ", clusterAnalysis=" + clusterAnalysis +
        ", dependencies=" + dependencies +
        ", jobName=" + jobName +
        '}';
  }

}
