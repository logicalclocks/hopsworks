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

package io.hops.hopsworks.common.featurestore.trainingdatasetjob;

import io.hops.hopsworks.common.util.Settings;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO for featurestore cloud jobs
 */

@XmlRootElement
public class TrainingDatasetJobDTO {
  
  @XmlElement(name="features", nillable = true)
  private List<String> features;
  @XmlElement(name="sql_query", nillable = true)
  private String sqlQuery;
  @XmlElement(name="training_dataset")
  private String trainingDataset;
  @XmlElement(name="featuregroups_version_dict")
  private String featuregroupsVersionDict;
  @XmlElement(name="join_key", nillable = true)
  private String joinKey;
  @XmlElement(name="description")
  private String description;
  @XmlElement(name="featurestore", nillable = true)
  private String featurestore;
  @XmlElement(name="data_format")
  private String dataFormat;
  @XmlElement(name="training_dataset_version")
  private int trainingDatasetVersion;
  @XmlElement(name="overwrite")
  private Boolean overwrite;
  @XmlElement(name="jobs")
  private List<String> jobs;
  @XmlElement(name="online")
  private Boolean online;
  @XmlElement(name="descriptive_statistics")
  private Boolean descriptiveStatistics;
  @XmlElement(name="feature_correlation")
  private Boolean featureCorrelation;
  @XmlElement(name="feature_histograms")
  private Boolean featureHistograms;
  @XmlElement(name="cluster_analysis")
  private Boolean clusterAnalysis;
  @XmlElement(name="stat_columns", nillable = true)
  private List<String> statColumns;
  @XmlElement(name="num_bins")
  private int numBins;
  @XmlElement(name="correlation_method")
  private String correlationMethod;
  @XmlElement(name="num_clusters")
  private int numClusters;
  @XmlElement(name="fixed")
  private Boolean fixed;
  @XmlElement(name="sink", nillable = true)
  private String sink;
  @XmlElement(name="am_cores")
  private int amCores = 1;
  @XmlElement(name="am_memory")
  private int amMemory = Settings.YARN_DEFAULT_APP_MASTER_MEMORY;;
  @XmlElement(name="executor_cores")
  private int executorCores = 1;
  @XmlElement(name="executor_memory")
  private int executorMemory = 4096;
  @XmlElement(name="max_executors")
  private int maxExecutors = 2;
  @XmlElement(name="path", nillable = true)
  private String path;
  

  public TrainingDatasetJobDTO(List<String> features, String sqlQuery, String trainingDataset,
    String featuregroupsVersionDict, String joinKey, String description, String featurestore, String dataFormat,
    int trainingDatasetVersion, Boolean overwrite, List<String> jobs, Boolean online, Boolean descriptiveStatistics,
    Boolean featureCorrelation, Boolean featureHistograms, Boolean clusterAnalysis, List<String> statColumns,
    int numBins, String correlationMethod, int numClusters, Boolean fixed, String sink, int amCores, int amMemory,
    int executorCores, int executorMemory, int maxExecutors, String path) {
    this.features = features;
    this.sqlQuery = sqlQuery;
    this.trainingDataset = trainingDataset;
    this.featuregroupsVersionDict = featuregroupsVersionDict;
    this.joinKey = joinKey;
    this.description = description;
    this.featurestore = featurestore;
    this.dataFormat = dataFormat;
    this.trainingDatasetVersion = trainingDatasetVersion;
    this.overwrite = overwrite;
    this.jobs = jobs;
    this.online = online;
    this.descriptiveStatistics = descriptiveStatistics;
    this.featureCorrelation = featureCorrelation;
    this.featureHistograms = featureHistograms;
    this.clusterAnalysis = clusterAnalysis;
    this.statColumns = statColumns;
    this.numBins = numBins;
    this.correlationMethod = correlationMethod;
    this.numClusters = numClusters;
    this.fixed = fixed;
    this.sink = sink;
    this.amCores = amCores;
    this.amMemory = amMemory;
    this.executorCores = executorCores;
    this.executorMemory = executorMemory;
    this.maxExecutors = maxExecutors;
    this.path = path;
  }
  
  public TrainingDatasetJobDTO() {
  }
  
  public List<String> getFeatures() {
    return features;
  }
  
  public void setFeatures(List<String> features) {
    this.features = features;
  }
  
  public String getSqlQuery() {
    return sqlQuery;
  }
  
  public void setSqlQuery(String features) {
    this.sqlQuery = sqlQuery;
  }
  
  public String getTrainingDataset() {
    return trainingDataset;
  }
  
  public void setTrainingDataset(String trainingDataset) {
    this.trainingDataset = trainingDataset;
  }
  
  public String getFeaturegroupsVersionDict() {
    return this.featuregroupsVersionDict;
  }
  
  public void setFeaturegroupsVersionDict(String featuregroupsVersionDict) {
    this.featuregroupsVersionDict = featuregroupsVersionDict;
  }
  
  public String getJoinKey() {
    return joinKey;
  }
  
  public void setJoinKey(String joinKey) {
    this.joinKey = joinKey;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getFeaturestore() {
    return featurestore;
  }
  
  public void setFeaturestore(String featurestore) {
    this.featurestore = featurestore;
  }
  
  public String getDataFormat() {
    return dataFormat;
  }
  
  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }
  
  public int getTrainingDatasetVersion() {
    return trainingDatasetVersion;
  }
  
  public void setTrainingDatasetVersion(int trainingDatasetVersion) {
    this.trainingDatasetVersion = trainingDatasetVersion;
  }
  
  public Boolean getOverwrite() {
    return overwrite;
  }
  
  public void setOverwrite(Boolean overwrite) {
    this.overwrite = overwrite;
  }
  
  public List<String> getJobs() {
    return jobs;
  }
  
  public void setJobs(List<String> jobs) {
    this.jobs = jobs;
  }
  
  public Boolean getOnline() {
    return online;
  }
  
  public void setOnline(Boolean online) {
    this.online = online;
  }
  
  public Boolean getDescriptiveStatistics() {
    return descriptiveStatistics;
  }
  
  public void setDescriptiveStatistics(Boolean descriptiveStatistics) {
    this.descriptiveStatistics = descriptiveStatistics;
  }
  
  public Boolean getFeatureCorrelation() {
    return featureCorrelation;
  }
  
  public void setFeatureCorrelation(Boolean featureCorrelation) {
    this.featureCorrelation = featureCorrelation;
  }
  
  public Boolean getFeatureHistograms() {
    return featureHistograms;
  }
  
  public void setFeatureHistograms(Boolean featureHistograms) {
    this.featureHistograms = featureHistograms;
  }
  
  public Boolean getClusterAnalysis() {
    return clusterAnalysis;
  }
  
  public void setClusterAnalysis(Boolean clusterAnalysis) {
    this.clusterAnalysis = clusterAnalysis;
  }
  
  public List<String> getStatColumns() {
    return statColumns;
  }
  
  public void setStatColumns(List<String> statColumns) {
    this.statColumns = statColumns;
  }
  
  public int getNumBins() {
    return numBins;
  }
  
  public void setNumBins(int numBins) {
    this.numBins = numBins;
  }
  
  public String getCorrelationMethod() {
    return correlationMethod;
  }
  
  public void setCorrelationMethod(String correlationMethod) {
    this.correlationMethod = correlationMethod;
  }
  
  public int getNumClusters() {
    return numClusters;
  }
  
  public void setNumClusters(int numClusters) {
    this.numClusters = numClusters;
  }
  
  public Boolean getFixed() {
    return fixed;
  }
  
  public void setFixed(Boolean fixed) {
    this.fixed = fixed;
  }
  
  public String getSink() {
    return sink;
  }
  
  public void setSink(String sink) {
    this.sink = sink;
  }
  
  public int getAmCores() {
    return amCores;
  }
  
  public void setAmCores(int amCores) {
    this.amCores = amCores;
  }
  
  public int getAmMemory() {
    return amMemory;
  }
  
  public void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }
  
  public int getExecutorCores() {
    return executorCores;
  }
  
  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }
  
  public int getExecutorMemory() {
    return executorMemory;
  }
  
  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }
  
  public int getMaxExecutors() {
    return maxExecutors;
  }
  
  public void setMaxExecutors(int maxExecutors) {
    this.maxExecutors = maxExecutors;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
}
