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

package io.hops.hopsworks.common.dao.featurestore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatistic;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticType;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsMetricValuesDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.CorrelationValueDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract storage entity in the featurestore. Contains the common fields and functionality between feature groups
 * and training dataset entities.
 */
@XmlRootElement
@XmlSeeAlso({FeaturegroupDTO.class, TrainingDatasetDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FeaturegroupDTO.class, name = "FeaturegroupDTO"),
    @JsonSubTypes.Type(value = TrainingDatasetDTO.class, name = "TrainingDatasetDTO")})
public abstract class FeaturestoreEntityDTO {
  private Integer featurestoreId;
  private String featurestoreName;
  private String description;
  private Date created;
  private String creator;
  private Integer jobId;
  private String jobName;
  private Date lastComputed;
  private String jobStatus;
  private Integer version;
  private DescriptiveStatsDTO descriptiveStatistics;
  private FeatureCorrelationMatrixDTO featureCorrelationMatrix;
  private FeatureDistributionsDTO featuresHistogram;
  private ClusterAnalysisDTO clusterAnalysis;
  private String name;
  private Integer id;
  private List<FeatureDTO> features;
  private String location;

  public FeaturestoreEntityDTO() {
  }

  public FeaturestoreEntityDTO(
      Integer featurestoreId, Date created,
      Users creator, Integer version, List<FeaturestoreStatistic> featurestoreStatistics, Jobs job,
      Integer id) {
    this.featurestoreId = featurestoreId;
    this.featurestoreName = null;
    this.description = null;
    this.created = created;
    this.creator = creator.getEmail();
    this.version = version;
    this.name = null;
    this.location = null;
    this.id = id;
    if (job != null) {
      this.jobId = job.getId();
      this.jobName = job.getName();
      extractLastComputed(job);
    } else {
      this.jobId = null;
      this.jobName = null;
      this.lastComputed = null;
      this.jobStatus = null;
    }
    this.clusterAnalysis = parseClusterAnalysis(featurestoreStatistics);
    this.featureCorrelationMatrix = parseFeatureCorrelation(featurestoreStatistics);
    this.descriptiveStatistics = parseDescriptiveStats(featurestoreStatistics);
    this.featuresHistogram = parseFeatureDistributions(featurestoreStatistics);
  }

  private void extractLastComputed(Jobs job) {
    Collection<Execution> executions = job.getExecutions();
    this.lastComputed = null;
    this.jobStatus = null;
    if (!executions.isEmpty()) {
      Optional<Execution> optionalMax = executions.stream().max(Comparator.comparing(Execution::getSubmissionTime));
      if (optionalMax.isPresent()) {
        Execution latestExecution = optionalMax.get();
        this.lastComputed = latestExecution.getSubmissionTime();
        this.jobStatus = latestExecution.getFinalStatus().toString();
      }
    }
  }

  private ClusterAnalysisDTO parseClusterAnalysis(List<FeaturestoreStatistic> featurestoreStatistics) {
    List<FeaturestoreStatistic> clusterAnalysisList = featurestoreStatistics.stream().filter(fss ->
        fss.getStatisticType() == FeaturestoreStatisticType.CLUSTERANALYSIS)
        .collect(Collectors.toList());
    if (clusterAnalysisList.isEmpty()) {
      return null;
    }
    FeaturestoreStatistic clusterAnalysisStatistic = clusterAnalysisList.get(0);
    return (ClusterAnalysisDTO) clusterAnalysisStatistic.getValue();
  }

  private FeatureCorrelationMatrixDTO parseFeatureCorrelation(List<FeaturestoreStatistic> featurestoreStatistics) {
    List<FeaturestoreStatistic> featureCorrelationList =
        featurestoreStatistics.stream().filter(fss ->
            fss.getStatisticType() == FeaturestoreStatisticType.FEATURECORRELATIONS)
            .collect(Collectors.toList());
    if (featureCorrelationList.isEmpty()) {
      return null;
    }
    HashMap<String, List<CorrelationValueDTO>> featureCorrelations = new HashMap<>();
    featureCorrelationList.stream().forEach(fc -> {
      List<CorrelationValueDTO> featureCorrelationValues =
          featureCorrelations.getOrDefault(fc.getName(), new ArrayList());
      featureCorrelationValues.add((CorrelationValueDTO) fc.getValue());
      featureCorrelations.put(fc.getName(), featureCorrelationValues);
    });
    List<FeatureCorrelationDTO> featureCorrelationDTOS = featureCorrelations.entrySet().stream().map(entry -> {
      FeatureCorrelationDTO featureCorrelationDTO = new FeatureCorrelationDTO();
      featureCorrelationDTO.setFeatureName(entry.getKey());
      featureCorrelationDTO.setCorrelationValues(entry.getValue());
      return featureCorrelationDTO;
    }).collect(Collectors.toList());
    FeatureCorrelationMatrixDTO featureCorrelationMatrixDTO = new FeatureCorrelationMatrixDTO();
    featureCorrelationMatrixDTO.setFeatureCorrelations(featureCorrelationDTOS);
    return featureCorrelationMatrixDTO;
  }

  private DescriptiveStatsDTO parseDescriptiveStats(List<FeaturestoreStatistic> featurestoreStatistics) {
    List<FeaturestoreStatistic> descriptiveStatisticsList =
        featurestoreStatistics.stream().filter(fss ->
            fss.getStatisticType() == FeaturestoreStatisticType.DESCRIPTIVESTATISTICS)
            .collect(Collectors.toList());
    if (descriptiveStatisticsList.isEmpty()) {
      return null;
    }
    List<DescriptiveStatsMetricValuesDTO> descriptiveStatsMetricValuesDTOS = descriptiveStatisticsList.stream()
        .map(fss -> (DescriptiveStatsMetricValuesDTO) fss.getValue()).collect(Collectors.toList());
    DescriptiveStatsDTO descriptiveStatsDTO = new DescriptiveStatsDTO();
    descriptiveStatsDTO.setDescriptiveStats(descriptiveStatsMetricValuesDTOS);
    return descriptiveStatsDTO;
  }

  private FeatureDistributionsDTO parseFeatureDistributions(List<FeaturestoreStatistic> featurestoreStatistics) {
    List<FeaturestoreStatistic> featureDistributions = featurestoreStatistics.stream().filter(fss ->
        fss.getStatisticType() == FeaturestoreStatisticType.FEATUREDISTRIBUTIONS)
        .collect(Collectors.toList());
    if (featureDistributions.isEmpty()) {
      return null;
    }
    List<FeatureDistributionDTO> featureDistributionDTOS = featureDistributions.stream()
        .map(fss -> (FeatureDistributionDTO) fss.getValue()).collect(Collectors.toList());
    FeatureDistributionsDTO featureDistributionsDTO = new FeatureDistributionsDTO();
    featureDistributionsDTO.setFeatureDistributions(featureDistributionDTOS);
    return featureDistributionsDTO;
  }

  @XmlElement
  public Date getCreated() {
    return created;
  }

  @XmlElement
  public String getCreator() {
    return creator;
  }

  @XmlElement(nillable = true)
  public Integer getJobId() {
    return jobId;
  }

  @XmlElement(nillable = true)
  public String getJobName() {
    return jobName;
  }

  @XmlElement(nillable = true)
  public Date getLastComputed() {
    return lastComputed;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  @XmlElement
  public Integer getVersion() {
    return version;
  }

  @XmlElement(nillable = true)
  public String getJobStatus() {
    return jobStatus;
  }

  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }

  @XmlElement
  public String getFeaturestoreName() {
    return featurestoreName;
  }

  @XmlElement(nillable = true)
  public FeatureCorrelationMatrixDTO getFeatureCorrelationMatrix() {
    return featureCorrelationMatrix;
  }

  @XmlElement(nillable = true)
  public FeatureDistributionsDTO getFeaturesHistogram() {
    return featuresHistogram;
  }

  @XmlElement(nillable = true)
  public ClusterAnalysisDTO getClusterAnalysis() {
    return clusterAnalysis;
  }

  @XmlElement(nillable = true)
  public DescriptiveStatsDTO getDescriptiveStatistics() {
    return descriptiveStatistics;
  }

  @XmlElement
  public String getName() {
    return name;
  }

  @XmlElement
  public Integer getId() {
    return id;
  }

  @XmlElement
  public List<FeatureDTO> getFeatures() {
    return features;
  }
  
  @XmlElement
  public String getLocation() {
    return location;
  }
  
  public void setLocation(String location) {
    this.location = location;
  }
  
  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }

  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public void setDescriptiveStatistics(
    DescriptiveStatsDTO descriptiveStatistics) {
    this.descriptiveStatistics = descriptiveStatistics;
  }
  
  public void setFeatureCorrelationMatrix(
    FeatureCorrelationMatrixDTO featureCorrelationMatrix) {
    this.featureCorrelationMatrix = featureCorrelationMatrix;
  }
  
  public void setFeaturesHistogram(
    FeatureDistributionsDTO featuresHistogram) {
    this.featuresHistogram = featuresHistogram;
  }
  
  public void setClusterAnalysis(
    ClusterAnalysisDTO clusterAnalysis) {
    this.clusterAnalysis = clusterAnalysis;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public void setJobId(Integer jobId) {
    this.jobId = jobId;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setLastComputed(Date lastComputed) {
    this.lastComputed = lastComputed;
  }

  public void setJobStatus(String jobStatus) {
    this.jobStatus = jobStatus;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "FeaturestoreEntityDTO{" +
        "featurestoreId=" + featurestoreId +
        ", featurestoreName='" + featurestoreName + '\'' +
        ", description='" + description + '\'' +
        ", created='" + created + '\'' +
        ", creator='" + creator + '\'' +
        ", jobId=" + jobId +
        ", jobName='" + jobName + '\'' +
        ", lastComputed='" + lastComputed + '\'' +
        ", jobStatus='" + jobStatus + '\'' +
        ", version=" + version +
        ", descriptiveStatistics=" + descriptiveStatistics +
        ", featureCorrelationMatrix=" + featureCorrelationMatrix +
        ", featuresHistogram=" + featuresHistogram +
        ", clusterAnalysis=" + clusterAnalysis +
        ", name='" + name + '\'' +
        ", id=" + id +
        '}';
  }
}
