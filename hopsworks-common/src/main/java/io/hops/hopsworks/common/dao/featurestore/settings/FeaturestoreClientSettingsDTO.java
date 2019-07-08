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

package io.hops.hopsworks.common.dao.featurestore.settings;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

/**
 * DTO containing the feature store client settings
 */
@XmlRootElement
public class FeaturestoreClientSettingsDTO {
  
  public static int FEATURESTORE_STATISTICS_MAX_CORRELATIONS= 50;
  public static String FEATURESTORE_REGEX = "^[a-zA-Z0-9_]+$";
  public static int STORAGE_CONNECTOR_NAME_MAX_LENGTH = 1000;
  public static int STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH = 1000;
  public static int JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH = 5000;
  public static int JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH = 2000;
  public static int S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH = 5000;
  public static int S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH = 1000;
  public static int S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH = 1000;
  public static int CACHED_FEATUREGROUP_NAME_MAX_LENGTH = 767;
  public static int CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = 256;
  public static int CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = 767;
  public static int CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = 256;
  public static int ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH = 1000;
  public static int ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = 1000;
  public static int ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = 1000;
  public static int ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = 10000;
  public static int ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH = 11000;
  public static List<String> TRAINING_DATASET_DATA_FORMATS = Arrays.asList(new String[]{"csv", "tfrecords",
    "parquet", "tsv", "hdf5", "npy", "orc", "avro", "image", "petastorm"});
  public static int EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH = 256;
  public static int HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH = 256;
  public static int TRAINING_DATASET_FEATURE_NAME_MAX_LENGTH = 1000;
  public static int TRAINING_DATASET_FEATURE_DESCRIPTION_MAX_LENGTH = 10000;
  
  
  public FeaturestoreClientSettingsDTO() {
  }
  
  @XmlElement
  public int getFeaturestoreStatisticsMaxCorrelations() {
    return FEATURESTORE_STATISTICS_MAX_CORRELATIONS;
  }
  
  @XmlElement
  public String getFeaturestoreRegex() {
    return FEATURESTORE_REGEX;
  }
  
  @XmlElement
  public int getStorageConnectorNameMaxLength() {
    return STORAGE_CONNECTOR_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getStorageConnectorDescriptionMaxLength() {
    return STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public int getJdbcStorageConnectorConnectionstringMaxLength() {
    return JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH;
  }
  
  @XmlElement
  public int getJdbcStorageConnectorArgumentsMaxLength() {
    return JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH;
  }
  
  @XmlElement
  public int getS3StorageConnectorBucketMaxLength() {
    return S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH;
  }
  
  @XmlElement
  public int getS3StorageConnectorAccesskeyMaxLength() {
    return S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH;
  }
  
  @XmlElement
  public int getS3StorageConnectorSecretkeyMaxLength() {
    return S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH;
  }
  
  @XmlElement
  public int getCachedFeaturegroupNameMaxLength() {
    return CACHED_FEATUREGROUP_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getCachedFeaturegroupDescriptionMaxLength() {
    return CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public int getCachedFeaturegroupFeatureNameMaxLength() {
    return CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getCachedFeaturegroupFeatureDescriptionMaxLength() {
    return CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public int getOnDemandFeaturegroupNameMaxLength() {
    return ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getOnDemandFeaturegroupDescriptionMaxLength() {
    return ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public int getOnDemandFeaturegroupFeatureNameMaxLength() {
    return ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getOnDemandFeaturegroupFeatureDescriptionMaxLength() {
    return ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public int getOnDemandFeaturegroupSqlQueryMaxLength() {
    return ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH;
  }
  
  @XmlElement
  public List<String> getTrainingDatasetDataFormats() {
    return TRAINING_DATASET_DATA_FORMATS;
  }
  
  @XmlElement
  public int getExternalTrainingDatasetNameMaxLength() {
    return EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getHopsfsTrainingDatasetNameMaxLength() {
    return HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH;
  }
  
  @XmlElement
  public int getTrainingDatasetFeatureNameMaxLength() {
    return TRAINING_DATASET_FEATURE_NAME_MAX_LENGTH;
  }
  @XmlElement
  public int getTrainingDatasetFeatureDescriptionMaxLength() {
    return TRAINING_DATASET_FEATURE_DESCRIPTION_MAX_LENGTH;
  }
  
  public void setFeaturestoreStatisticsMaxCorrelations(int featurestoreStatisticsMaxCorrelations) {
    FEATURESTORE_STATISTICS_MAX_CORRELATIONS = featurestoreStatisticsMaxCorrelations;
  }
  
  public void setFeaturestoreRegex(String featurestoreRegex) {
    FEATURESTORE_REGEX = featurestoreRegex;
  }
  
  public void setStorageConnectorNameMaxLength(int storageConnectorNameMaxLength) {
    STORAGE_CONNECTOR_NAME_MAX_LENGTH = storageConnectorNameMaxLength;
  }
  
  public void setStorageConnectorDescriptionMaxLength(int storageConnectorDescriptionMaxLength) {
    STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH = storageConnectorDescriptionMaxLength;
  }
  
  public void setJdbcStorageConnectorConnectionstringMaxLength(
    int jdbcStorageConnectorConnectionstringMaxLength) {
    JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH = jdbcStorageConnectorConnectionstringMaxLength;
  }
  
  public void setJdbcStorageConnectorArgumentsMaxLength(int jdbcStorageConnectorArgumentsMaxLength) {
    JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH = jdbcStorageConnectorArgumentsMaxLength;
  }
  
  public void setS3StorageConnectorBucketMaxLength(int s3StorageConnectorBucketMaxLength) {
    S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH = s3StorageConnectorBucketMaxLength;
  }
  
  public void setS3StorageConnectorAccesskeyMaxLength(int s3StorageConnectorAccesskeyMaxLength) {
    S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH = s3StorageConnectorAccesskeyMaxLength;
  }
  
  public void setS3StorageConnectorSecretkeyMaxLength(int s3StorageConnectorSecretkeyMaxLength) {
    S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH = s3StorageConnectorSecretkeyMaxLength;
  }
  
  public void setCachedFeaturegroupNameMaxLength(int cachedFeaturegroupNameMaxLength) {
    CACHED_FEATUREGROUP_NAME_MAX_LENGTH = cachedFeaturegroupNameMaxLength;
  }
  
  public void setCachedFeaturegroupDescriptionMaxLength(int cachedFeaturegroupDescriptionMaxLength) {
    CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = cachedFeaturegroupDescriptionMaxLength;
  }
  
  public void setCachedFeaturegroupFeatureNameMaxLength(int cachedFeaturegroupFeatureNameMaxLength) {
    CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = cachedFeaturegroupFeatureNameMaxLength;
  }
  
  public void setCachedFeaturegroupFeatureDescriptionMaxLength(
    int cachedFeaturegroupFeatureDescriptionMaxLength) {
    CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = cachedFeaturegroupFeatureDescriptionMaxLength;
  }
  
  public void setOnDemandFeaturegroupNameMaxLength(int onDemandFeaturegroupNameMaxLength) {
    ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH = onDemandFeaturegroupNameMaxLength;
  }
  
  public void setOnDemandFeaturegroupDescriptionMaxLength(int onDemandFeaturegroupDescriptionMaxLength) {
    ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = onDemandFeaturegroupDescriptionMaxLength;
  }
  
  public void setOnDemandFeaturegroupFeatureNameMaxLength(int onDemandFeaturegroupFeatureNameMaxLength) {
    ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = onDemandFeaturegroupFeatureNameMaxLength;
  }
  
  public void setOnDemandFeaturegroupFeatureDescriptionMaxLength(
    int onDemandFeaturegroupFeatureDescriptionMaxLength) {
    ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = onDemandFeaturegroupFeatureDescriptionMaxLength;
  }
  
  public void setOnDemandFeaturegroupSqlQueryMaxLength(int onDemandFeaturegroupSqlQueryMaxLength) {
    ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH = onDemandFeaturegroupSqlQueryMaxLength;
  }
  
  public void setTrainingDatasetDataFormats(List<String> trainingDatasetDataFormats) {
    TRAINING_DATASET_DATA_FORMATS = trainingDatasetDataFormats;
  }
  
  public void setExternalTrainingDatasetNameMaxLength(int externalTrainingDatasetNameMaxLength) {
    EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH = externalTrainingDatasetNameMaxLength;
  }
  
  public void setHopsfsTrainingDatasetNameMaxLength(int hopsfsTrainingDatasetNameMaxLength) {
    HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH = hopsfsTrainingDatasetNameMaxLength;
  }
  
  public void setTrainingDatasetFeatureNameMaxLength(int trainingDatasetFeatureNameMaxLength) {
    TRAINING_DATASET_FEATURE_NAME_MAX_LENGTH = trainingDatasetFeatureNameMaxLength;
  }
  
  public void setTrainingDatasetFeatureDescriptionMaxLength(int trainingDatasetFeatureDescriptionMaxLength) {
    TRAINING_DATASET_FEATURE_DESCRIPTION_MAX_LENGTH = trainingDatasetFeatureDescriptionMaxLength;
  }
}
