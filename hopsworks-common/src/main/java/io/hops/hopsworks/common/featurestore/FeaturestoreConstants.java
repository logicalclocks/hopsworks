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

package io.hops.hopsworks.common.featurestore;

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Constants for the Feature Store Service
 */
public class FeaturestoreConstants {

  private FeaturestoreConstants() {
  }

  public static final Pattern FEATURESTORE_REGEX = Pattern.compile("^(?=.{1,63}$)([a-z]{1}[a-z0-9_]*)$");
  public static final Pattern KEYWORDS_REGEX = Pattern.compile("^[a-zA-Z0-9_]{1,63}$");
  public static final int FEATURESTORE_ENTITY_NAME_MAX_LENGTH = 63; // limited by NDB due to online fg
  public static final int FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH = 256; // can possibly 1000, but the one for
  // features is limited to 256
  public static final int STORAGE_CONNECTOR_NAME_MAX_LENGTH = 1000;
  public static final int STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH = 1000;
  public static final int JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH = 5000;
  public static final int STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH = 2000;
  public static final int S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH = 5000;
  public static final int S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH = 1000;
  public static final int S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH = 1000;
  public static final int S3_STORAGE_SERVER_ENCRYPTION_KEY_MAX_LENGTH = 1000;
  public static final int KAFKA_STORAGE_CONNECTOR_BOOTSTRAP_MAX_LENGTH = 1000;
  public static final int ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH = 11000;
  public static final List<String> TRAINING_DATASET_DATA_FORMATS = Arrays.asList("csv", "tfrecords",
    "tfrecord", "parquet", "tsv", "hdf5", "npy", "orc", "avro", "image", "petastorm");
  public static final String JDBC_CONNECTOR_TYPE = FeaturestoreConnectorType.JDBC.name();
  public static final String HOPSFS_CONNECTOR_TYPE = FeaturestoreConnectorType.HOPSFS.name();
  public static final String S3_CONNECTOR_TYPE = FeaturestoreConnectorType.S3.name();
  public static final String REDSHIFT_CONNECTOR_TYPE = FeaturestoreConnectorType.REDSHIFT.name();
  public static final String CACHED_FEATUREGROUP_DTO_TYPE = "cachedFeaturegroupDTO";
  public static final String STREAM_FEATUREGROUP_DTO_TYPE = "streamFeatureGroupDTO";
  public static final String ON_DEMAND_FEATUREGROUP_DTO_TYPE = "onDemandFeaturegroupDTO";
  public static final String HOPSFS_TRAINING_DATASET_TYPE = TrainingDatasetType.HOPSFS_TRAINING_DATASET.name();
  public static final String EXTERNAL_TRAINING_DATASET_TYPE = TrainingDatasetType.EXTERNAL_TRAINING_DATASET.name();
  public static final String S3_CONNECTOR_DTO_TYPE = "featurestoreS3ConnectorDTO";
  public static final String JDBC_CONNECTOR_DTO_TYPE = "featurestoreJdbcConnectorDTO";
  public static final String REDSHIFT_CONNECTOR_DTO_TYPE = "featurestoreRedshiftConnectorDTO";
  public static final String HOPSFS_CONNECTOR_DTO_TYPE = "featurestoreHopsfsConnectorDTO";
  public static final String FEATUREGROUP_TYPE = "FEATURE GROUP";
  public static final String TRAINING_DATASET_TYPE = "TRAINING DATASET";
  public static final List<String> SUGGESTED_HIVE_FEATURE_TYPES = Arrays.asList("INT",
    "BIGINT", "FLOAT", "DOUBLE", "DECIMAL", "TIMESTAMP", "DATE", "STRING",
    "BOOLEAN", "BINARY",
    "ARRAY <TINYINT>", "ARRAY <SMALLINT>", "ARRAY <INT>", "ARRAY <BIGINT>",
    "ARRAY <FLOAT>", "ARRAY <DOUBLE>", "ARRAY <DECIMAL>", "ARRAY <TIMESTAMP>",
    "ARRAY <DATE>", "ARRAY <STRING>",
    "ARRAY <BOOLEAN>", "ARRAY <BINARY>", "ARRAY <ARRAY <FLOAT> >",
    "ARRAY <ARRAY <INT> >", "ARRAY <ARRAY <STRING> >",
    "STRUCT < label: STRING, index: INT >");
  public static final String S3_BUCKET_TRAINING_DATASETS_FOLDER = "TRAINING_DATASETS";
  public static final List<String> FEATURE_IMPORT_CONNECTORS
    = Arrays.asList(S3_CONNECTOR_TYPE, JDBC_CONNECTOR_TYPE, REDSHIFT_CONNECTOR_TYPE);
  public static final String ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE = "<SECRETPASSWORD>";
  public static final String ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX = "_onlinefeaturestore";
  public static final String ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG = "password";
  public static final String ONLINE_FEATURE_STORE_JDBC_USER_ARG = "user";
  public static final String ONLINE_FEATURE_STORE_JDBC_DRIVER_ARG = "driver";
  public static final List<String> SUGGESTED_MYSQL_DATA_TYPES = Arrays.asList("INT",
    "TINYINT", "SMALLINT", "BIGINT", "FLOAT", "DOUBLE", "DECIMAL",
    "DATE", "TIMESTAMP", "VARCHAR(25)", "VARCHAR(125)", "VARCHAR(225)",
    "VARCHAR(500)", "VARCHAR(1000)", "VARCHAR(2000)", "VARCHAR(5000)", "VARCHAR(10000)",
    "VARBINARY(100)", "VARBINARY(500)", "VARBINARY(1000)", "BLOB", "TEXT");
  public static final Integer MAX_MYSQL_COLUMNS = 500;
  public static final Integer MAX_MYSQL_COLUMN_SIZE = 30000;
  public static final Integer MAX_MYSQL_PRIMARY_KEY_SIZE = 4000;
  public static final List<String> SUPPORTED_MYSQL_PRIMARY_KEYS = Arrays.asList("INT",
    "TINYINT", "SMALLINT", "BIGINT", "VARCHAR", "VARBINARY");
  public static final List<String> SUPPORTED_HUDI_PARTITION_KEYS = Arrays.asList("INT",
          "BIGINT", "FLOAT", "DOUBLE", "DATE", "STRING");
  public static final int ONLINE_FEATURESTORE_USERNAME_MAX_LENGTH = 32;
  public static final int ONLINE_FEATURESTORE_PW_LENGTH = 32;
  public static final String FEATURESTORE_HIVE_DB_SUFFIX = "_featurestore";
  public static final List<String> OFFLINE_JDBC_CONNECTOR_ARGS = Arrays.asList(
          "sslTrustStore", "trustStorePassword", "sslKeyStore", "keyStorePassword");
  public static final String STORAGE_CONNECTOR_SUBDIR = "storage_connector_resources";
  public static final List<String> TRANSFORMATION_FUNCTION_OUTPUT_TYPES = Arrays.asList("STRING", "BINARY", "BYTE",
    "SHORT", "INT", "LONG", "FLOAT", "DOUBLE", "TIMESTAMP", "DATE", "BOOLEAN");
  public static final List<String> EVENT_TIME_FEATURE_TYPES = Arrays.asList("TIMESTAMP", "DATE", "BIGINT");
  public static final List<String> BUILT_IN_TRANSFORMATION_FUNCTION_NAMES = Arrays.asList(
    "min_max_scaler", "standard_scaler", "robust_scaler", "label_encoder"
  );
  public static final String BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_MIN_MAX_SCALER =
    "{\n" +
      "  \"module_imports\": \"\",\n" +
      "  \"transformer_code\": \"def min_max_scaler(value, min_value, max_value):\\n" +
      "    if value is None:\\n        return None\\n    elif float(max_value - min_value) == float(0):\\n" +
      "        return float(0)\\n    else:\\n        " +
      "return float((value - min_value) / (max_value - min_value))\\n\"\n" +
      "}";
  public static final String BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_STANDARD_SCALER =
    "{\n" +
      "  \"module_imports\": \"\",\n" +
      "  \"transformer_code\": \"def standard_scaler(value, mean, std_dev):\\n    if value is None:\\n" +
      "        return None\\n    elif float(std_dev) == float(0):\\n        return float(0)\\n" +
      "    else:\\n        return float((value - mean) / std_dev)\\n\"\n" +
      "}";
  public static final String BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_ROBUST_SCALER =
    "{\n" +
      "  \"module_imports\": \"\",\n" +
      "  \"transformer_code\": \"def robust_scaler(value, p25, p50, p75):\\n    if value is None:\\n        " +
      "return None\\n    elif float(p75 - p25) == float(0):\\n        return float(0)\\n    else:\\n" +
      "        return float((value - p50) / (p75 - p25))\\n\"\n" +
      "}";
  public static final String BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_LABEL_ENCODER =
    "{\"module_imports\": \"\", \"transformer_code\": \"# label encoder\\n" +
      "def label_encoder(value, value_to_index):\\n" +
      "    # define a mapping of values to integers\\n" +
      "    return value_to_index[value]\"}";
  public static final int MAX_CHARACTERS_IN_EXPECTATION_SUITE_NAME = 63;
  public static final int MAX_CHARACTERS_IN_EXPECTATION_SUITE_META = 1000;
  public static final int MAX_CHARACTERS_IN_GE_CLOUD_ID = 200;
  public static final int MAX_CHARACTERS_IN_DATA_ASSET_TYPE = 50;
  public static final int MAX_CHARACTERS_IN_EXPECTATION_META = 1000;
  public static final int MAX_CHARACTERS_IN_EXPECTATION_KWARGS = 5000;
  public static final int MAX_CHARACTERS_IN_EXPECTATION_EXPECTATION_TYPE = 150;
  public static final int MAX_CHARACTERS_IN_VALIDATION_REPORT_EVALUATION_PARAMETERS = 1000;
  public static final int MAX_CHARACTERS_IN_VALIDATION_REPORT_STATISTICS = 1000;
  public static final int MAX_CHARACTERS_IN_VALIDATION_REPORT_META = 2000;
  public static final int MAX_CHARACTERS_IN_VALIDATION_RESULT_META = 1000;
  public static final int MAX_CHARACTERS_IN_VALIDATION_RESULT_EXCEPTION_INFO = 1000;
  public static final int MAX_CHARACTERS_IN_VALIDATION_RESULT_EXPECTATION_CONFIG = 2150;
  public static final int MAX_CHARACTERS_IN_VALIDATION_RESULT_RESULT_FIELD = 1000;
  public static final  String TRAINING_DATASETS_FOLDER = "TRAINING_DATASETS";
  public static final  String DEFAULT_REDSHIFT_DRIVER = "com.amazon.redshift.jdbc42.Driver";
}
