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

package io.hops.hopsworks.common.featurestore.importjob;

import com.google.gson.annotations.SerializedName;
import io.hops.hopsworks.common.util.Settings;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class FeaturegroupImportJobDTO implements Serializable {

  @SerializedName("type")
  private ImportType type;

  @XmlElement(name = "storage_connector")
  @SerializedName("storage_connector")
  private String storageConnector;
  private String path;
  private String featuregroup;
  @XmlElement(name = "primary_key")
  @SerializedName("primary_key")
  private List<String> primaryKey;
  private String description;
  private String featurestore;
  @XmlElement(name = "featuregroup_version")
  @SerializedName("featuregroup_version")
  private Integer featuregroupVersion;
  private String jobs;
  @XmlElement(name = "descriptive_statistics")
  @SerializedName("descriptive_statistics")
  private Boolean descriptiveStatistics;
  @XmlElement(name = "feature_correlation")
  @SerializedName("feature_correlation")
  private Boolean featureCorrelation;
  @XmlElement(name = "feature_histograms")
  @SerializedName("feature_histograms")
  private Boolean featureHistograms;
  @XmlElement(name = "cluster_analysis")
  @SerializedName("cluster_analysis")
  private Boolean clusterAnalysis;
  @XmlElement(name = "stats_column")
  @SerializedName("stats_column")
  private List<String> statsColumn;
  @XmlElement(name = "num_bins")
  @SerializedName("num_bins")
  private Integer numBins;
  @XmlElement(name = "corr_method")
  @SerializedName("corr_method")
  private String corrMethod;
  @XmlElement(name = "num_clusters")
  @SerializedName("num_clusters")
  private Integer num_clusters;
  @XmlElement(name = "partition_by")
  @SerializedName("partition_by")
  private List<String> partitionBy;
  @XmlElement(name = "data_format")
  @SerializedName("data_format")
  private String dataFormat;
  private boolean online;
  @XmlElement(name = "online_types")
  @SerializedName("online_types")
  private Map<String, String> onlineTypes = new HashMap<>();
  private boolean offline;
  @XmlElement(name="am_cores")
  private int amCores = 1;
  @XmlElement(name="am_memory")
  private int amMemory = Settings.YARN_DEFAULT_APP_MASTER_MEMORY;
  @XmlElement(name="executor_cores")
  private int executorCores = 1;
  @XmlElement(name="executor_memory")
  private int executorMemory = 4096;
  @XmlElement(name="max_executors")
  private int maxExecutors = Settings.SPARK_MAX_EXECS;


  public FeaturegroupImportJobDTO() {
    // Empty constructor
  }

  public FeaturegroupImportJobDTO(ImportType type, String storageConnector, String path, String featuregroup,
                                  List<String> primaryKey, String description, String featurestore,
                                  Integer featuregroupVersion, String jobs, Boolean descriptiveStatistics,
                                  Boolean featureCorrelation, Boolean featureHistograms, Boolean clusterAnalysis,
                                  List<String> statsColumn, Integer numBins, String corrMethod, Integer num_clusters,
                                  List<String> partitionBy, String dataFormat, boolean online,
                                  Map<String, String> onlineTypes, boolean offline, int amCores, int amMemory,
                                  int executorCores, int executorMemory, int maxExecutors) {
    this.type = type;
    this.storageConnector = storageConnector;
    this.path = path;
    this.featuregroup = featuregroup;
    this.primaryKey = primaryKey;
    this.description = description;
    this.featurestore = featurestore;
    this.featuregroupVersion = featuregroupVersion;
    this.jobs = jobs;
    this.descriptiveStatistics = descriptiveStatistics;
    this.featureCorrelation = featureCorrelation;
    this.featureHistograms = featureHistograms;
    this.clusterAnalysis = clusterAnalysis;
    this.statsColumn = statsColumn;
    this.numBins = numBins;
    this.corrMethod = corrMethod;
    this.num_clusters = num_clusters;
    this.partitionBy = partitionBy;
    this.dataFormat = dataFormat;
    this.online = online;
    this.onlineTypes = onlineTypes;
    this.offline = offline;
    this.amCores = amCores;
    this.amMemory = amMemory;
    this.executorCores = executorCores;
    this.executorMemory = executorMemory;
    this.maxExecutors = maxExecutors;
  }

  public ImportType getType() {
    return type;
  }

  public void setType(ImportType type) {
    this.type = type;
  }

  public String getStorageConnector() {
    return storageConnector;
  }

  public void setStorageConnector(String storageConnector) {
    this.storageConnector = storageConnector;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(String featuregroup) {
    this.featuregroup = featuregroup;
  }

  public List<String> getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(List<String> primaryKey) {
    this.primaryKey = primaryKey;
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

  public Integer getFeaturegroupVersion() {
    return featuregroupVersion;
  }

  public void setFeaturegroupVersion(Integer featuregroupVersion) {
    this.featuregroupVersion = featuregroupVersion;
  }

  public String getJobs() {
    return jobs;
  }

  public void setJobs(String jobs) {
    this.jobs = jobs;
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

  public List<String> getStatsColumn() {
    return statsColumn;
  }

  public void setStatsColumn(List<String> statsColumn) {
    this.statsColumn = statsColumn;
  }

  public Integer getNumBins() {
    return numBins;
  }

  public void setNumBins(Integer numBins) {
    this.numBins = numBins;
  }

  public String getCorrMethod() {
    return corrMethod;
  }

  public void setCorrMethod(String corrMethod) {
    this.corrMethod = corrMethod;
  }

  public Integer getNum_clusters() {
    return num_clusters;
  }

  public void setNum_clusters(Integer num_clusters) {
    this.num_clusters = num_clusters;
  }

  public List<String> getPartitionBy() {
    return partitionBy;
  }

  public void setPartitionBy(List<String> partitionBy) {
    this.partitionBy = partitionBy;
  }

  public String getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public Map<String, String> getOnlineTypes() {
    return onlineTypes;
  }

  public void setOnlineTypes(Map<String, String> onlineTypes) {
    this.onlineTypes = onlineTypes;
  }

  public boolean isOffline() {
    return offline;
  }

  public void setOffline(boolean offline) {
    this.offline = offline;
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
}
