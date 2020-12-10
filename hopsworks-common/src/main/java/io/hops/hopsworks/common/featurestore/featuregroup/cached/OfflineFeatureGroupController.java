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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
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
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private Configuration metastoreConf;

  @PostConstruct
  public void init() {
    metastoreConf = MetastoreConf.newMetastoreConf();
    metastoreConf.addResource(new Path(settings.getHiveConfPath()));
  }

  private static final String COMMENT = "comment";
  private static final int CONNECTION_TIMEOUT = 600000;

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

  public void createHiveTable(Featurestore featurestore, String tableName, String tableDesc,
                              List<FeatureGroupFeatureDTO> featureGroupFeatureDTOList, Project project,
                              Users user, Formats format)
      throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    Table table = getEmptyTable(dbName, tableName, hdfsUsersController.getHdfsUserName(project, user), format);
    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);

    // add table description
    table.getParameters().put(COMMENT, tableDesc);

    // Create Schema
    List<SQLDefaultConstraint> defaultConstraints = new ArrayList<>();
    for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOList) {
      FieldSchema fieldSchema = new FieldSchema(featureGroupFeatureDTO.getName(),
          // need to lowercase the type
          featureGroupFeatureDTO.getType().toLowerCase(), featureGroupFeatureDTO.getDescription());
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

    createTable(client, table, defaultConstraints, project, user);
    finalizeMetastoreOperation(project, user, client);
  }
  
  public void alterHiveTableDescription(Featurestore featurestore, String tableName, String description,
    Project project, Users user) throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    Table table = getTable(client, dbName, tableName, project, user);
    table.getParameters().put(COMMENT, description);
    alterTable(client, table, project, user);
    finalizeMetastoreOperation(project, user, client);
  }

  public void alterHiveTableFeatures(Featurestore featurestore, String tableName,
    List<FeatureGroupFeatureDTO> featureDTOs, Project project, Users user) throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());

    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    Table table = getTable(client, dbName, tableName, project, user);
 
    // modify columns here
    List<SQLDefaultConstraint> defaultConstraints = getDefaultConstraints(client, featurestore, tableName, project,
      user);
    for (FeatureGroupFeatureDTO featureDTO : featureDTOs) {
      table.getSd().addToCols(
        new FieldSchema(featureDTO.getName(), featureDTO.getType().toLowerCase(), featureDTO.getDescription()));
      if (featureDTO.getDefaultValue() != null) {
        defaultConstraints.add(new SQLDefaultConstraint(table.getCatName(), table.getDbName(),
          table.getTableName(), featureDTO.getName(), featureDTO.getDefaultValue(),
          dbName + "_" + tableName + "_" + featureDTO.getName() + "_dc", true, false,
          false));
      }
    }
    alterTable(client, table, project, user);
    addDefaultConstraints(client, defaultConstraints, project, user);
    finalizeMetastoreOperation(project, user, client);
  }

  private void createTable(ThriftHiveMetastore.Client client, Table table,
                           List<SQLDefaultConstraint> defaultConstraints, Project project, Users user)
      throws FeaturestoreException {
    try {
      client.create_table_with_constraints(table, null, null, null, null, defaultConstraints, null);
    } catch (TException e) {
      finalizeMetastoreOperation(project, user, client);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.SEVERE,
          "Error creating feature group table in the Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }
  
  public List<SQLDefaultConstraint> getDefaultConstraints(Featurestore featurestore, String tableName, Project project,
                                                           Users user) throws FeaturestoreException {
    ThriftHiveMetastore.Client client = getMetaStoreClient(project, user);
    List<SQLDefaultConstraint> constraints = getDefaultConstraints(client, featurestore, tableName, project, user);
    finalizeMetastoreOperation(project, user, client);
    return constraints;
  }
  
  private List<SQLDefaultConstraint> getDefaultConstraints(ThriftHiveMetastore.Client client, Featurestore featurestore,
                                                          String tableName, Project project, Users user)
      throws FeaturestoreException {
    String dbName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    return getDefaultConstraints(client, project, user, "hive", dbName, tableName);
  }
  
  private List<SQLDefaultConstraint> getDefaultConstraints(ThriftHiveMetastore.Client client, Project project,
                                                           Users user, String catName, String dbName,
                                                           String tableName)
      throws FeaturestoreException {
    try {
      DefaultConstraintsRequest constraintRequest = new DefaultConstraintsRequest(catName, dbName, tableName);
      return client.get_default_constraints(constraintRequest).getDefaultConstraints();
    } catch (TException e) {
      finalizeMetastoreOperation(project, user, client);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_FEATURE_GROUP_METADATA,
        Level.SEVERE, "Error getting feature group default constraints from the Hive Metastore: " + e.getMessage(),
        e.getMessage(), e);
    }
  }

  private void alterTable(ThriftHiveMetastore.Client client, Table table, Project project, Users user)
    throws FeaturestoreException {
    try {
      client.alter_table_with_cascade(table.getDbName(), table.getTableName(), table, true);
    } catch (TException e) {
      finalizeMetastoreOperation(project, user, client);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_ALTER_FEAUTURE_GROUP_METADATA,
        Level.SEVERE, "Error altering feature group table in the Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }

  private void addDefaultConstraints(ThriftHiveMetastore.Client client, List<SQLDefaultConstraint> defaultConstraints,
                                     Project project, Users user) throws FeaturestoreException {
    try {
      AddDefaultConstraintRequest constraintRequest = new AddDefaultConstraintRequest();
      constraintRequest.setDefaultConstraintCols(defaultConstraints);
      client.add_default_constraint(constraintRequest);
    } catch (TException e) {
      finalizeMetastoreOperation(project, user, client);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_ALTER_FEAUTURE_GROUP_METADATA,
        Level.SEVERE, "Error adding default constraints to feature group in the Hive Metastore: " + e.getMessage(),
        e.getMessage(), e);
    }
  }

  private Table getTable(ThriftHiveMetastore.Client client, String dbName, String tableName,  Project project,
                         Users user) throws FeaturestoreException {
    try {
      return client.get_table(dbName, tableName);
    } catch (TException e) {
      finalizeMetastoreOperation(project, user, client);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_GET_FEATURE_GROUP_METADATA,
        Level.SEVERE, "Error getting feature group table from Hive Metastore: " + e.getMessage(), e.getMessage(), e);
    }
  }
  
  private ThriftHiveMetastore.Client getMetaStoreClient(Project project, Users user) throws FeaturestoreException {
    try {
      return openMetastoreClient(project, user);
    } catch (ServiceException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_HIVE_METASTORE_CLIENT,
        Level.SEVERE, "Error opening the Hive Metastore client: " + e.getMessage(), e.getMessage(), e);
    }
  }

  private void finalizeMetastoreOperation(Project project, Users user, ThriftHiveMetastore.Client client) {
    certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    if (client != null) {
      try {
        client.shutdown();
      } catch (TException e) {
        LOGGER.log(Level.SEVERE, "Error closing Metastore connection", e);
      }
    }
  }

  public void dropFeatureGroup(String dbName, String tableName, Project project, Users user)
      throws FeaturestoreException, ServiceException, IOException {
    ThriftHiveMetastore.Client client = null;
    try {
      client = openMetastoreClient(project, user);
      client.drop_table(dbName, tableName, true);
    } catch (TException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP, Level.SEVERE,
          "Error dropping feature group in the Hive Metastore: " +  e.getMessage(), e.getMessage(), e);
    } finally {
      finalizeMetastoreOperation(project, user, client);
    }
  }

  // Here we can't use the HiveMetaStoreClient.java wrapper as we would need to export environment variables and so on
  // instead we assemble directly the thirft client, which is what the HiveMetaStoreClient does behind the scenes.
  private ThriftHiveMetastore.Client openMetastoreClient(Project project, Users user)
      throws ServiceException, IOException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    ThriftHiveMetastore.Client client = null;

    try {
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      CertificateMaterializer.CryptoMaterial userMaterial =
          certificateMaterializer.getUserMaterial(user.getUsername(), project.getName());

      // read Password
      String password = String.copyValueOf(userMaterial.getPassword());

      // Get metastore service information from consul
      Service metastoreService = serviceDiscoveryController
          .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.HIVE_METASTORE);

      TTransport transport;
      if (settings.getHopsRpcTls()) {
        // Setup secure connection with the Hive metastore.
        TSSLTransportFactory.TSSLTransportParameters params =
            new TSSLTransportFactory.TSSLTransportParameters();
        params.setTrustStore(certificateMaterializer.getUserTransientTruststorePath(project, user), password);
        params.setKeyStore(certificateMaterializer.getUserTransientKeystorePath(project, user), password);

        transport = TSSLTransportFactory.getClientSocket(metastoreService.getAddress(),
            metastoreService.getPort(), CONNECTION_TIMEOUT, params);
      } else {
        transport = new TSocket(metastoreService.getAddress(), metastoreService.getPort(), CONNECTION_TIMEOUT);
      }

      TProtocol protocol = new TBinaryProtocol(transport);
      client = new ThriftHiveMetastore.Client(protocol);

      // Open transport
      if (!transport.isOpen()) {
        transport.open();
      }

      // Set the UGI on the metastore side
      client.set_ugi(hdfsUsername, new ArrayList<>());

      if (settings.getHopsRpcTls()) {
        // Send the certificate to the metastore so it can operate with the fs.
        client.set_crypto(userMaterial.getKeyStore(), password, userMaterial.getTrustStore(), password, false);
      }
    } catch (CryptoPasswordNotFoundException | ServiceDiscoveryException | TException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.METASTORE_CONNECTION_ERROR, Level.SEVERE,
          "Hive metastore connection error", e.getMessage(), e);
    }

    return client;
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
