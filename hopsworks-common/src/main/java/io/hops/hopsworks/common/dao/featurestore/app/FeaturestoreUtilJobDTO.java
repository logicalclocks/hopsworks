package io.hops.hopsworks.common.dao.featurestore.app;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * DTO for featurestore util jobs
 */
@XmlRootElement
public class FeaturestoreUtilJobDTO {

  private String fileName;
  private List<FeatureDTO> features;
  private List<FeaturegroupDTO> featuregroups;
  private String featurestore;
  private String trainingDataset;
  private String featuregroup;
  private String joinKey;
  private String description;
  private String dataFormat;
  private int version;
  private Boolean descriptiveStats = false;
  private Boolean featureCorrelation = false;
  private Boolean clusterAnalysis = false;
  private Boolean featureHistograms = false;
  private List<String> statColumns;
  private String operation;
  private String sqlQuery;
  private String hiveDatabase;
  private String jdbcString;
  private List<String> jdbcArguments;

  public FeaturestoreUtilJobDTO(
      List<FeatureDTO> features, List<FeaturegroupDTO> featuregroups, String featurestore, String trainingDataset,
      String featuregroup, String joinKey, String description, String dataFormat, int version, Boolean descriptiveStats,
      Boolean featureCorrelation, Boolean clusterAnalysis, Boolean featureHistograms, List<String> statColumns,
      String operation, String sqlQuery, String hiveDatabase, String jdbcString, List<String> jdbcArguments,
      String fileName) {
    this.features = features;
    this.featuregroups = featuregroups;
    this.featurestore = featurestore;
    this.trainingDataset = trainingDataset;
    this.featuregroup = featuregroup;
    this.joinKey = joinKey;
    this.description = description;
    this.dataFormat = dataFormat;
    this.version = version;
    this.descriptiveStats = descriptiveStats;
    this.featureCorrelation = featureCorrelation;
    this.clusterAnalysis = clusterAnalysis;
    this.featureHistograms = featureHistograms;
    this.statColumns = statColumns;
    this.operation = operation;
    this.sqlQuery = sqlQuery;
    this.hiveDatabase = hiveDatabase;
    this.jdbcString = jdbcString;
    this.jdbcArguments = jdbcArguments;
    this.fileName = fileName;
  }

  public FeaturestoreUtilJobDTO() {
  }

  @XmlElement
  public List<FeatureDTO> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureDTO> features) {
    this.features = features;
  }

  @XmlElement
  public List<FeaturegroupDTO> getFeaturegroups() {
    return featuregroups;
  }

  public void setFeaturegroups(List<FeaturegroupDTO> featuregroups) {
    this.featuregroups = featuregroups;
  }

  @XmlElement
  public String getFeaturestore() {
    return featurestore;
  }

  public void setFeaturestore(String featurestore) {
    this.featurestore = featurestore;
  }

  @XmlElement
  public String getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(String trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  @XmlElement
  public String getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(String featuregroup) {
    this.featuregroup = featuregroup;
  }

  @XmlElement
  public String getJoinKey() {
    return joinKey;
  }

  public void setJoinKey(String joinKey) {
    this.joinKey = joinKey;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElement
  public String getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  @XmlElement
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @XmlElement
  public Boolean getDescriptiveStats() {
    return descriptiveStats;
  }

  public void setDescriptiveStats(Boolean descriptiveStats) {
    this.descriptiveStats = descriptiveStats;
  }

  @XmlElement
  public Boolean getFeatureCorrelation() {
    return featureCorrelation;
  }

  public void setFeatureCorrelation(Boolean featureCorrelation) {
    this.featureCorrelation = featureCorrelation;
  }

  @XmlElement
  public Boolean getClusterAnalysis() {
    return clusterAnalysis;
  }

  public void setClusterAnalysis(Boolean clusterAnalysis) {
    this.clusterAnalysis = clusterAnalysis;
  }

  @XmlElement
  public Boolean getFeatureHistograms() {
    return featureHistograms;
  }

  public void setFeatureHistograms(Boolean featureHistograms) {
    this.featureHistograms = featureHistograms;
  }

  @XmlElement
  public List<String> getStatColumns() {
    return statColumns;
  }

  public void setStatColumns(List<String> statColumns) {
    this.statColumns = statColumns;
  }

  @XmlElement
  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  @XmlElement
  public String getSqlQuery() {
    return sqlQuery;
  }

  public void setSqlQuery(String sqlQuery) {
    this.sqlQuery = sqlQuery;
  }

  @XmlElement
  public String getHiveDatabase() {
    return hiveDatabase;
  }

  public void setHiveDatabase(String hiveDatabase) {
    this.hiveDatabase = hiveDatabase;
  }

  @XmlElement
  public String getJdbcString() {
    return jdbcString;
  }

  public void setJdbcString(String jdbcString) {
    this.jdbcString = jdbcString;
  }

  @XmlElement
  public List<String> getJdbcArguments() {
    return jdbcArguments;
  }

  public void setJdbcArguments(List<String> jdbcArguments) {
    this.jdbcArguments = jdbcArguments;
  }

  @XmlElement
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
