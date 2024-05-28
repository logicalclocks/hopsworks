/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.AddDefaultConstraintRequest;
import org.apache.hadoop.hive.metastore.api.DefaultConstraintsRequest;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.SQLDefaultConstraint;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.SkewedInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.hadoop.hive.metastore.api.hive_metastoreConstants;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.thrift.TException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OfflineFeatureGroupController {

  private final static Logger LOGGER = Logger.getLogger(OfflineFeatureGroupController.class.getName());

  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private HiveController hiveController;

  private Configuration metastoreConf;

  @PostConstruct
  public void init() {
    metastoreConf = MetastoreConf.newMetastoreConf();
    metastoreConf.addResource(new Path(settings.getHiveConfPath()));
  }

  public enum Formats {
    ORC("org.apache.hadoop.hive.ql.io.orc.OrcInputFormat",
        "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat",
        "org.apache.hadoop.hive.ql.io.orc.OrcSerde"),
    PARQUET("org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat",
        "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat",
        "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"),
    AVRO("org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat",
        "org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat",
        "org.apache.hadoop.hive.serde2.avro.AvroSerDe"),
    HUDI("org.apache.hudi.hadoop.HoodieParquetInputFormat",
        "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat",
        "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe");

    private String inputFormat;
    private String outputFormat;
    private String serde;

    Formats(String inputFormat, String outputFormat, String serde) {
      this.inputFormat = inputFormat;
      this.outputFormat = outputFormat;
      this.serde = serde;
    }

    public String getInputFormat() {
      return inputFormat;
    }

    public String getOutputFormat() {
      return outputFormat;
    }

    public String getSerde() {
      return serde;
    }
  }

  public void createHiveTable(Featurestore featurestore, String tableName,
                              List<FeatureGroupFeatureDTO> featureGroupFeatureDTOList, Project project,
                              Users user, Formats format)
      throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    Table table = getEmptyTable(dbName, tableName, hdfsUsersController.getHdfsUserName(project, user), format);

    // Create Schema
    List<SQLDefaultConstraint> defaultConstraints = new ArrayList<>();
    for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOList) {
      FieldSchema fieldSchema = new FieldSchema(featureGroupFeatureDTO.getName(),
          // need to lowercase the type
          featureGroupFeatureDTO.getType().toLowerCase(), null);
      if (featureGroupFeatureDTO.getPartition()) {
        table.addToPartitionKeys(fieldSchema);
      } else {
        table.getSd().addToCols(fieldSchema);
      }

      if (featureGroupFeatureDTO.getDefaultValue() != null) {
        defaultConstraints.add(new SQLDefaultConstraint(table.getCatName(), table.getDbName(),
          table.getTableName(), featureGroupFeatureDTO.getName(), featureGroupFeatureDTO.getDefaultValue(),
          dbName + "_" + tableName + "_" + featureGroupFeatureDTO.getName() + "_dc", true, false,
          false));
      }
    }

    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    try {
      createTable(client, table, defaultConstraints);
    } finally {
      hiveController.finalizeMetastoreOperation(project, user, client);
    }
  }

  public void alterHiveTableFeatures(Featurestore featurestore, String tableName,
    List<FeatureGroupFeatureDTO> featureDTOs, Project project, Users user) throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());

    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    try {
      Table table = getTable(client, dbName, tableName);

      // modify columns here
      List<SQLDefaultConstraint> defaultConstraints = getDefaultConstraints(client, "hive", dbName, tableName);
      for (FeatureGroupFeatureDTO featureDTO : featureDTOs) {
        table.getSd().addToCols(
            new FieldSchema(featureDTO.getName(), featureDTO.getType().toLowerCase(), null));
        if (featureDTO.getDefaultValue() != null) {
          defaultConstraints.add(new SQLDefaultConstraint(table.getCatName(), table.getDbName(),
              table.getTableName(), featureDTO.getName(), featureDTO.getDefaultValue(),
              dbName + "_" + tableName + "_" + featureDTO.getName() + "_dc", true, false,
              false));
        }
      }
      alterTable(client, table);
      addDefaultConstraints(client, defaultConstraints);
    } finally {
      hiveController.finalizeMetastoreOperation(project, user, client);
    }
  }

  public List<FeatureGroupFeatureDTO> getSchema(Featurestore featurestore, String tableName,
                                                Project project, Users user) throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> featureSchema = new ArrayList<>();

    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    Table table;
    List<FieldSchema> schema;
    List<SQLDefaultConstraint> defaultConstraints;
    try {
      table = getTable(client, dbName, tableName);
      schema = getFields(client, dbName, tableName);
      defaultConstraints = getDefaultConstraints(client, "hive", dbName, tableName);
    } catch (FeaturestoreException e) {
      if (e.getCause() instanceof NoSuchObjectException) {
        // If the hive table doesn't exist return the feature group without features
        // (Otherwise the user would not be able to get/delete the broken feature group in the UI/HSFS,
        // since he would receive 500 responses to GET requests)
        LOGGER.log(Level.SEVERE, "Feature group Hive table does not exist", e);
        return featureSchema;
      }
      throw e;
    } finally {
      hiveController.finalizeMetastoreOperation(project, user, client);
    }

    // Setup a map of constraint values for easy access
    Map<String, String> defaultConstraintsMap = defaultConstraints.stream()
        .collect(Collectors.toMap(SQLDefaultConstraint::getColumn_name, SQLDefaultConstraint::getDefault_value));

    // Partitions are listed first
    for (FieldSchema fieldSchema : table.getPartitionKeys()) {
      featureSchema.add(new FeatureGroupFeatureDTO(fieldSchema.getName(), fieldSchema.getType(), true,
          defaultConstraintsMap.get(fieldSchema.getName())));
    }

    // take care of the remaining of the schema
    for (FieldSchema fieldSchema : schema) {
      featureSchema.add(new FeatureGroupFeatureDTO(fieldSchema.getName(), fieldSchema.getType(), false,
          defaultConstraintsMap.get(fieldSchema.getName())));
    }

    return featureSchema;
  }

  private void createTable(ThriftHiveMetastore.Client client, Table table,
                           List<SQLDefaultConstraint> defaultConstraints) throws FeaturestoreException {
    try {
      client.create_table_with_constraints(table, null, null, null, null, defaultConstraints, null);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "Error creating feature group table in the Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }
  
  private List<SQLDefaultConstraint> getDefaultConstraints(ThriftHiveMetastore.Client client,
                                                           String catName, String dbName, String tableName)
      throws FeaturestoreException {
    try {
      DefaultConstraintsRequest constraintRequest = new DefaultConstraintsRequest(catName, dbName, tableName);
      return client.get_default_constraints(constraintRequest).getDefaultConstraints();
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_FEATURE_GROUP_METADATA,
        Level.SEVERE, "Error getting feature group default constraints from the Hive Metastore: " + e.getMessage(),
        e.getMessage(), e);
    }
  }

  private void alterTable(ThriftHiveMetastore.Client client, Table table) throws FeaturestoreException {
    try {
      client.alter_table_with_cascade(table.getDbName(), table.getTableName(), table, true);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_ALTER_FEAUTURE_GROUP_METADATA,
        Level.SEVERE, "Error altering feature group table in the Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }

  private void addDefaultConstraints(ThriftHiveMetastore.Client client, List<SQLDefaultConstraint> defaultConstraints)
      throws FeaturestoreException {
    try {
      AddDefaultConstraintRequest constraintRequest = new AddDefaultConstraintRequest();
      constraintRequest.setDefaultConstraintCols(defaultConstraints);
      client.add_default_constraint(constraintRequest);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_ALTER_FEAUTURE_GROUP_METADATA,
        Level.SEVERE, "Error adding default constraints to feature group in the Hive Metastore: " + e.getMessage(),
        e.getMessage(), e);
    }
  }

  private Table getTable(ThriftHiveMetastore.Client client, String dbName, String tableName)
      throws FeaturestoreException {
    try {
      return client.get_table(dbName, tableName);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_FEATURE_GROUP_METADATA,
        Level.SEVERE, "Error getting feature group table from Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }

  private List<FieldSchema> getFields(ThriftHiveMetastore.Client client, String dbName, String tableName)
      throws FeaturestoreException {
    try {
      return client.get_fields(dbName, tableName);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_FEATURE_GROUP_METADATA,
          Level.SEVERE, "Error getting feature group table from Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }
  
  private ThriftHiveMetastore.Client getMetaStoreClient(Project project, Users user) throws FeaturestoreException {
    try {
      return hiveController.openUserMetastoreClient(project, user);
    } catch (ServiceException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_HIVE_METASTORE_CLIENT,
        Level.SEVERE, "Error opening the Hive Metastore client: " + e.getMessage(), e.getMessage(), e);
    }
  }

  public void dropFeatureGroup(String dbName, String tableName, Project project, Users user)
      throws FeaturestoreException, ServiceException, IOException {
    ThriftHiveMetastore.Client client = null;
    try {
      client = hiveController.openUserMetastoreClient(project, user);
      client.drop_table(dbName, tableName, true);
    } catch (NoSuchObjectException e) {
      LOGGER.log(Level.INFO, "Hive table being deleted does not exist", e);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "Error dropping feature group in the Hive Metastore: " +  e.getMessage(), e.getMessage(), e);
    } finally {
      hiveController.finalizeMetastoreOperation(project, user, client);
    }
  }

  private Table getEmptyTable(String databaseName, String tableName, String username, Formats format) {
    StorageDescriptor sd = new StorageDescriptor();
    {
      sd.setSerdeInfo(new SerDeInfo());
      sd.setNumBuckets(-1);
      sd.setBucketCols(new ArrayList<>());
      sd.setCols(new ArrayList<>());
      sd.setParameters(new HashMap<>());
      sd.setSortCols(new ArrayList<>());
      sd.getSerdeInfo().setParameters(new HashMap<>());
      // We have to use MetadataTypedColumnsetSerDe because LazySimpleSerDe does
      // not support a table with no columns.
      sd.getSerdeInfo().setSerializationLib(format.getSerde());
      sd.setInputFormat(format.getInputFormat());
      sd.setOutputFormat(format.getOutputFormat());
      sd.getSerdeInfo().getParameters().put(serdeConstants.SERIALIZATION_FORMAT, "1");
      SkewedInfo skewInfo = new SkewedInfo();
      skewInfo.setSkewedColNames(new ArrayList<>());
      skewInfo.setSkewedColValues(new ArrayList<>());
      skewInfo.setSkewedColValueLocationMaps(new HashMap<>());
      sd.setSkewedInfo(skewInfo);
    }

    Table t = new Table();
    {
      t.setSd(sd);
      t.setPartitionKeys(new ArrayList<>());
      t.setParameters(new HashMap<>());
      t.setTableType(TableType.MANAGED_TABLE.toString());
      t.setDbName(databaseName);
      t.setTableName(tableName);
      t.setOwner(username);
      // set create time
      t.setCreateTime((int) (System.currentTimeMillis() / 1000));
    }

    // Explictly set the bucketing version
    t.getParameters().put(hive_metastoreConstants.TABLE_BUCKETING_VERSION, "2");
    return t;
  }
}
