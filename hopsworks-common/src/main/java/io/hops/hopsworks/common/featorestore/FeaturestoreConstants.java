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

package io.hops.hopsworks.common.featorestore;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorType;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetType;

import java.util.Arrays;
import java.util.List;

/**
 * Constants for the Feature Store Service
 */
public class FeaturestoreConstants {
  
  private FeaturestoreConstants() {
  }
  
  public static final int FEATURESTORE_STATISTICS_MAX_CORRELATIONS= 50;
  public static final String FEATURESTORE_REGEX = "^[a-zA-Z0-9_]+$";
  public static final int STORAGE_CONNECTOR_NAME_MAX_LENGTH = 1000;
  public static final int STORAGE_CONNECTOR_DESCRIPTION_MAX_LENGTH = 1000;
  public static final int JDBC_STORAGE_CONNECTOR_CONNECTIONSTRING_MAX_LENGTH = 5000;
  public static final int JDBC_STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH = 2000;
  public static final int S3_STORAGE_CONNECTOR_BUCKET_MAX_LENGTH = 5000;
  public static final int S3_STORAGE_CONNECTOR_ACCESSKEY_MAX_LENGTH = 1000;
  public static final int S3_STORAGE_CONNECTOR_SECRETKEY_MAX_LENGTH = 1000;
  public static final int CACHED_FEATUREGROUP_NAME_MAX_LENGTH = 767;
  public static final int CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = 256;
  public static final int CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = 767;
  public static final int CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = 256;
  public static final int ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH = 1000;
  public static final int ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH = 1000;
  public static final int ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH = 1000;
  public static final int ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH = 10000;
  public static final int ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH = 11000;
  public static final List<String> TRAINING_DATASET_DATA_FORMATS = Arrays.asList(new String[]{"csv", "tfrecords",
    "parquet", "tsv", "hdf5", "npy", "orc", "avro", "image", "petastorm"});
  public static final int EXTERNAL_TRAINING_DATASET_NAME_MAX_LENGTH = 256;
  public static final int HOPSFS_TRAINING_DATASET_NAME_MAX_LENGTH = 256;
  public static final int TRAINING_DATASET_FEATURE_NAME_MAX_LENGTH = 1000;
  public static final int TRAINING_DATASET_FEATURE_DESCRIPTION_MAX_LENGTH = 10000;
  public static final String ON_DEMAND_FEATUREGROUP_TYPE = FeaturegroupType.ON_DEMAND_FEATURE_GROUP.name();
  public static final String CACHED_FEATUREGROUP_TYPE = FeaturegroupType.CACHED_FEATURE_GROUP.name();
  public static final String JDBC_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.JDBC.name();
  public static final String HOPSFS_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.HOPSFS.name();
  public static final String S3_CONNECTOR_TYPE = FeaturestoreStorageConnectorType.S3.name();
  public static final String CACHED_FEATUREGROUP_DTO_TYPE = "cachedFeaturegroupDTO";
  public static final String ON_DEMAND_FEATUREGROUP_DTO_TYPE = "onDemandFeaturegroupDTO";
  public static final String HOPSFS_TRAINING_DATASET_TYPE = TrainingDatasetType.HOPSFS_TRAINING_DATASET.name();
  public static final String EXTERNAL_TRAINING_DATASET_TYPE = TrainingDatasetType.EXTERNAL_TRAINING_DATASET.name();
  public static final String HOPSFS_TRAINING_DATASET_DTO_TYPE = "hopsfsTrainingDatasetDTO";
  public static final String EXTERNAL_TRAINING_DATASET_DTO_TYPE = "externalTrainingDatasetDTO";
  public static final int TRAINING_DATASET_DESCRIPTION_MAX_LENGTH = 10000;
  public static final String S3_CONNECTOR_DTO_TYPE = "featurestoreS3ConnectorDTO";
  public static final String JDBC_CONNECTOR_DTO_TYPE = "featurestoreJdbcConnectorDTO";
  public static final String HOPSFS_CONNECTOR_DTO_TYPE = "featurestoreHopsfsConnectorDTO";
  public static final String FEATUREGROUP_TYPE = "FEATURE GROUP";
  public static final String TRAINING_DATASET_TYPE = "TRAINING DATASET";
  public static final List<String> SUGGESTED_FEATURE_TYPES = Arrays.asList(new String[]{
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
  public static final String FEATURESTORE_UTIL_4J_MAIN_CLASS = "io.hops.examples.featurestore_util4j.Main";
  public static final String FEATURESTORE_UTIL_4J_ARGS_DATASET = "Resources";
  public static final String FEATURESTORE_UTIL_PYTHON_MAIN_CLASS = "org.apache.spark.deploy.PythonRunner";
  public static final String FEATURESTORE_UTIL_4J_EXECUTABLE =
    "/user/spark/hops-examples-featurestore-util4j-1.0.0-SNAPSHOT.jar";
  public static final String FEATURESTORE_UTIL_PYTHON_EXECUTABLE =
    "/user/spark/featurestore_util.py";
  public static final String S3_BUCKET_TRAINING_DATASETS_FOLDER = "TRAINING_DATASETS";
  public static final List<String> FEATURE_IMPORT_CONNECTORS = Arrays.asList(new String[]{S3_CONNECTOR_TYPE});
  
}
