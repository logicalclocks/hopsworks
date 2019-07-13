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

import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

/**
 * DTO containing the feature store client settings (source of truth for JS client, Python Client, Java Client, and
 * Scala Client to the Feature Store
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
  public static String ON_DEMAND_FEATUREGROUP_TYPE = FeaturegroupType.ON_DEMAND_FEATURE_GROUP.name();
  public static String CACHED_FEATUREGROUP_TYPE = FeaturegroupType.CACHED_FEATURE_GROUP.name();
  public static String JDBC_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.JDBC.name();
  public static String HOPSFS_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.HopsFS.name();
  public static String S3_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.S3.name();
  public static String CACHED_FEATUREGROUP_DTO_TYPE = "cachedFeaturegroupDTO";
  public static String ON_DEMAND_FEATUREGROUP_DTO_TYPE = "onDemandFeaturegroupDTO";
  public static String HOPSFS_TRAINING_DATASET_TYPE = TrainingDatasetType.HOPSFS_TRAINING_DATASET.name();
  public static String EXTERNAL_TRAINING_DATASET_TYPE = TrainingDatasetType.EXTERNAL_TRAINING_DATASET.name();
  public static String HOPSFS_TRAINING_DATASET_DTO_TYPE = "hopsfsTrainingDatasetDTO";
  public static String EXTERNAL_TRAINING_DATASET_DTO_TYPE = "externalTrainingDatasetDTO";
  public static int TRAINING_DATASET_DESCRIPTION_MAX_LENGTH = 10000;
  public static String S3_CONNECTOR_DTO_TYPE = "featurestoreS3ConnectorDTO";
  public static String JDBC_CONNECTOR_DTO_TYPE = "featurestoreJdbcConnectorDTO";
  public static String HOPSFS_CONNECTOR_DTO_TYPE = "featurestoreHopsfsConnectorDTO";
  public static String FEATUREGROUP_TYPE = "FEATURE GROUP";
  public static String TRAINING_DATASET_TYPE = "TRAINING DATASET";
  public static List<String> SUGGESTED_FEATURE_TYPES = Arrays.asList(new String[]{
    "None","TINYINT", "SMALLINT", "INT", "BIGINT", "FLOAT", "DOUBLE",
    "DECIMAL", "TIMESTAMP", "DATE", "STRING",
    "BOOLEAN", "BINARY",
    "ARRAY <TINYINT>", "ARRAY <SMALLINT>", "ARRAY <INT>", "ARRAY <BIGINT>",
    "ARRAY <FLOAT>", "ARRAY <DOUBLE>", "ARRAY <DECIMAL>", "ARRAY <TIMESTAMP>",
    "ARRAY <DATE>", "ARRAY <STRING>",
    "ARRAY <BOOLEAN>", "ARRAY <BINARY>", "ARRAY <ARRAY <FLOAT> >",
    "ARRAY <ARRAY <INT> >", "ARRAY <ARRAY <STRING> >",
    "MAP <FLOAT, FLOAT>", "MAP <FLOAT, STRING>", "MAP <FLOAT, INT>", "MAP <FLOAT, BINARY>",
    "MAP <INT, INT>", "MAP <INT, STRING>", "MAP <INT, BINARY>", "MAP <INT, FLOAT>",
    "MAP <INT, ARRAY <FLOAT> >",
    "STRUCT < label: STRING, index: INT >", "UNIONTYPE < STRING, INT>"
  });
  public static String FEATURESTORE_UTIL_4J_MAIN_CLASS = "io.hops.examples.featurestore_util4j.Main";
  public static String FEATURESTORE_UTIL_4J_ARGS_DATASET = "Resources";
  public static String FEATURESTORE_UTIL_PYTHON_MAIN_CLASS = "org.apache.spark.deploy.PythonRunner";
  public static String FEATURESTORE_UTIL_4J_EXECUTABLE =
      "/user/spark/hops-examples-featurestore-util4j-1.0.0-SNAPSHOT.jar";
  public static String FEATURESTORE_UTIL_PYTHON_EXECUTABLE =
      "/user/spark/featurestore_util.py";
  
  
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
  
  @XmlElement
  public String getOnDemandFeaturegroupType() {
    return ON_DEMAND_FEATUREGROUP_TYPE;
  }
  
  @XmlElement
  public String getCachedFeaturegroupType() {
    return CACHED_FEATUREGROUP_TYPE;
  }
  
  @XmlElement
  public String getJdbcConnectorType() {
    return JDBC_CONNECTOR_TYPE;
  }
  
  @XmlElement
  public String getHopsfsConnectorType() {
    return HOPSFS_CONNECTOR_TYPE;
  }
  
  @XmlElement
  public String getS3ConnectorType() {
    return S3_CONNECTOR_TYPE;
  }
  
  @XmlElement
  public String getCachedFeaturegroupDtoType() {
    return CACHED_FEATUREGROUP_DTO_TYPE;
  }
  
  @XmlElement
  public String getOnDemandFeaturegroupDtoType() {
    return ON_DEMAND_FEATUREGROUP_DTO_TYPE;
  }
  
  @XmlElement
  public String getHopsfsTrainingDatasetType() {
    return HOPSFS_TRAINING_DATASET_TYPE;
  }
  
  @XmlElement
  public String getExternalTrainingDatasetType() {
    return EXTERNAL_TRAINING_DATASET_TYPE;
  }
  
  @XmlElement
  public String getHopsfsTrainingDatasetDtoType() {
    return HOPSFS_TRAINING_DATASET_DTO_TYPE;
  }
  
  @XmlElement
  public String getExternalTrainingDatasetDtoType() {
    return EXTERNAL_TRAINING_DATASET_DTO_TYPE;
  }
  
  @XmlElement
  public int getTrainingDatasetDescriptionMaxLength() {
    return TRAINING_DATASET_DESCRIPTION_MAX_LENGTH;
  }
  
  @XmlElement
  public String getS3ConnectorDtoType() {
    return S3_CONNECTOR_DTO_TYPE;
  }
  
  @XmlElement
  public String getJdbcConnectorDtoType() {
    return JDBC_CONNECTOR_DTO_TYPE;
  }
  
  @XmlElement
  public String getHopsfsConnectorDtoType() {
    return HOPSFS_CONNECTOR_DTO_TYPE;
  }
  
  @XmlElement
  public String getFeaturegroupType() {
    return FEATUREGROUP_TYPE;
  }
  
  @XmlElement
  public String getTrainingDatasetType() {
    return TRAINING_DATASET_TYPE;
  }
  
  @XmlElement
  public List<String> getSuggestedFeatureTypes() {
    return SUGGESTED_FEATURE_TYPES;
  }

  @XmlElement
  public String getFeaturestoreUtil4jMainClass() {
    return FEATURESTORE_UTIL_4J_MAIN_CLASS;
  }

  @XmlElement
  public String getFeaturestoreUtil4jArgsDataset() {
    return FEATURESTORE_UTIL_4J_ARGS_DATASET;
  }

  @XmlElement
  public String getFeaturestoreUtilPythonMainClass() {
    return FEATURESTORE_UTIL_PYTHON_MAIN_CLASS;
  }

  @XmlElement
  public String getFeaturestoreUtil4jExecutable() {
    return FEATURESTORE_UTIL_4J_EXECUTABLE;
  }

  @XmlElement
  public String getFeaturestoreUtilPythonExecutable() {
    return FEATURESTORE_UTIL_PYTHON_EXECUTABLE;
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
  
  public void setOnDemandFeaturegroupType(String onDemandFeaturegroupType) {
    ON_DEMAND_FEATUREGROUP_TYPE = onDemandFeaturegroupType;
  }
  
  public void setCachedFeaturegroupType(String cachedFeaturegroupType) {
    CACHED_FEATUREGROUP_TYPE = cachedFeaturegroupType;
  }
  
  public void setJdbcConnectorType(String jdbcConnectorType) {
    JDBC_CONNECTOR_TYPE = jdbcConnectorType;
  }
  
  public void setHopsfsConnectorType(String hopsfsConnectorType) {
    HOPSFS_CONNECTOR_TYPE = hopsfsConnectorType;
  }
  
  public void setS3ConnectorType(String s3ConnectorType) {
    S3_CONNECTOR_TYPE = s3ConnectorType;
  }
  
  public void setCachedFeaturegroupDtoType(String cachedFeaturegroupDtoType) {
    CACHED_FEATUREGROUP_DTO_TYPE = cachedFeaturegroupDtoType;
  }
  
  public void setOnDemandFeaturegroupDtoType(String onDemandFeaturegroupDtoType) {
    ON_DEMAND_FEATUREGROUP_DTO_TYPE = onDemandFeaturegroupDtoType;
  }
  
  public void setHopsfsTrainingDatasetType(String hopsfsTrainingDatasetType) {
    HOPSFS_TRAINING_DATASET_TYPE = hopsfsTrainingDatasetType;
  }
  
  public void setExternalTrainingDatasetType(String externalTrainingDatasetType) {
    EXTERNAL_TRAINING_DATASET_TYPE = externalTrainingDatasetType;
  }
  
  public void setHopsfsTrainingDatasetDtoType(String hopsfsTrainingDatasetDtoType) {
    HOPSFS_TRAINING_DATASET_DTO_TYPE = hopsfsTrainingDatasetDtoType;
  }
  
  public void setExternalTrainingDatasetDtoType(String externalTrainingDatasetDtoType) {
    EXTERNAL_TRAINING_DATASET_DTO_TYPE = externalTrainingDatasetDtoType;
  }
  
  public void setTrainingDatasetDescriptionMaxLength(int trainingDatasetDescriptionMaxLength) {
    TRAINING_DATASET_DESCRIPTION_MAX_LENGTH = trainingDatasetDescriptionMaxLength;
  }
  
  public void setS3ConnectorDtoType(String s3ConnectorDtoType) {
    S3_CONNECTOR_DTO_TYPE = s3ConnectorDtoType;
  }
  
  public void setJdbcConnectorDtoType(String jdbcConnectorDtoType) {
    JDBC_CONNECTOR_DTO_TYPE = jdbcConnectorDtoType;
  }
  
  public void setHopsfsConnectorDtoType(String hopsfsConnectorDtoType) {
    HOPSFS_CONNECTOR_DTO_TYPE = hopsfsConnectorDtoType;
  }
  
  public void setFeaturegroupType(String featuregroupType) {
    FEATUREGROUP_TYPE = featuregroupType;
  }
  
  public void setTrainingDatasetType(String trainingDatasetType) {
    TRAINING_DATASET_TYPE = trainingDatasetType;
  }
  
  public void setSuggestedFeatureTypes(List<String> suggestedFeatureTypes) {
    SUGGESTED_FEATURE_TYPES = suggestedFeatureTypes;
  }

  public void setFeaturestoreUtil4jMainClass(String featurestoreUtil4jMainClass) {
    FEATURESTORE_UTIL_4J_MAIN_CLASS = featurestoreUtil4jMainClass;
  }

  public void setFeaturestoreUtil4jArgsDataset(String featurestoreUtil4jArgsDataset) {
    FEATURESTORE_UTIL_4J_ARGS_DATASET = featurestoreUtil4jArgsDataset;
  }

  public void setFeaturestoreUtilPythonMainClass(String featurestoreUtilPythonMainClass) {
    FEATURESTORE_UTIL_PYTHON_MAIN_CLASS = featurestoreUtilPythonMainClass;
  }

  public void setFeaturestoreUtil4jExecutable(String featurestoreUtil4jExecutable) {
    FEATURESTORE_UTIL_4J_EXECUTABLE = featurestoreUtil4jExecutable;
  }

  public void setFeaturestoreUtilPythonExecutable(String featurestoreUtilPythonExecutable) {
    FEATURESTORE_UTIL_PYTHON_EXECUTABLE = featurestoreUtilPythonExecutable;
  }
}
