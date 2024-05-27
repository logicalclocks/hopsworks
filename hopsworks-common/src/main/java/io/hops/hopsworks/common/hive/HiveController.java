/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.hive;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.log.operation.OperationType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.HiveTags;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless(name = "HiveController")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HiveController {

  public final static String HIVE_JDBC_PREFIX = "jdbc:hopshive://";
  public final static String HIVE_DRIVER = "io.hops.hive.jdbc.HiveDriver";
  public final static int CONNECTION_TIMEOUT = 600000;

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private BaseHadoopClientsService bhcs;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private HopsFSProvenanceController fsProvenanceCtrl;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private CertificateMaterializer certificateMaterializer;

  private final static Logger logger = Logger.getLogger(HiveController.class.getName());

  /**
   * Creates a Hopsworks dataset of a Hive database
   *
   * @param project the project of the hive database and the place where the dataset will reside
   * @param user the user making the request
   * @param dfso dfso
   * @param dbName name of the hive database
   * @throws IOException
   */
  public void createDatasetDb(Project project, Users user, DistributedFileSystemOps dfso,
                              String dbName, ProvTypeDTO metaStatus) throws IOException {
    createDatasetDb(project, user, dfso, dbName, DatasetType.HIVEDB, null, metaStatus);
  }

  /**
   * Creates a Hopsworks dataset of a Hive database
   *
   * @param project the project of the hive database and the place where the dataset will reside
   * @param user the user making the request
   * @param dfso dfso
   * @param dbName name of the hive database
   * @param datasetType the type of database (regular HiveDB or a FeaturestoreDB)
   * @param featurestore the featurestore with extended metadata of the dataset in case of type featurestoreDB,
   *                     defaults to null.
   * @throws IOException
   */
  public void createDatasetDb(Project project, Users user, DistributedFileSystemOps dfso, String dbName,
    DatasetType datasetType, Featurestore featurestore, ProvTypeDTO metaStatus)
    throws IOException {
    if(datasetType != DatasetType.HIVEDB && datasetType != DatasetType.FEATURESTORE) {
      throw new IllegalArgumentException("Invalid dataset type for hive database");
    }

    // Persist Hive db as dataset in the Hopsworks database
    // Make the dataset editable by owners by default
    Dataset dbDataset = new Dataset(project, getDbDirName(dbName), DatasetAccessPermission.EDITABLE_BY_OWNERS);
    dbDataset.setDsType(datasetType);
    dbDataset.setSearchable(true);
    dbDataset.setFeatureStore(featurestore);
    datasetFacade.persistDataset(dbDataset);

    // Hive database names are case insensitive and lower case
    Path dbPath = getDbPath(dbName);

    try {
      // Assign database directory to the user and project group
      hdfsUsersController.createDatasetGroupsAndSetPermissions(user, project, dbDataset, dbPath, dfso);
  
      fsProvenanceCtrl.updateHiveDatasetProvCore(project, dbPath.toString(), metaStatus, dfso);
      Inode dbInode = inodeController.getInodeAtPath(dbPath.toString());
      datasetController.logDataset(project, dbDataset, dbInode, OperationType.Add);
      activityFacade.persistActivity(ActivityFacade.NEW_DATA + dbDataset.getName(), project, user,
        ActivityFlag.DATASET);

      // Set the default quota
      switch (datasetType) {
        case HIVEDB:
          dfso.setHdfsSpaceQuota(dbPath, settings.getHiveDbDefaultQuota());
          break;
        case FEATURESTORE:
          dfso.setHdfsSpaceQuota(dbPath, settings.getFeaturestoreDbDefaultQuota());
          break;
      }
    } catch (IOException | ProvenanceException e) {
      logger.log(Level.SEVERE, "Cannot assign Hive database directory " + dbPath +
          " to correct user/group. Trace: " + e);

      // Remove the database directory and cleanup the metadata
      try {
        dfso.rm(dbPath, true);
      } catch (IOException rmEx) {
        // Nothing we can really do here
        logger.log(Level.SEVERE, "Cannot delete Hive database directory: " + dbPath + " Trace: " + rmEx);
      }
      throw new IOException(e);
    }
  }

  public void dropDatabases(Project project, DistributedFileSystemOps dfso, boolean forceCleanup)
      throws IOException, ServiceException {
    // To avoid case sensitive bugs, check if the project has a Hive database
    Dataset projectDs = datasetController
      .getByProjectAndDsName(project, this.settings.getHiveWarehouse(), project.getName().toLowerCase() + ".db");
    Dataset featurestoreDs = datasetController.getByProjectAndDsName(project, this.settings.getHiveWarehouse(),
      project.getName().toLowerCase() + FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX + ".db");
    if ((projectDs != null && projectDs.getDsType() == DatasetType.HIVEDB) || forceCleanup) {
      dropDatabase(project.getName(), project, dfso);
    }

    if ((featurestoreDs != null && featurestoreDs.getDsType() == DatasetType.FEATURESTORE) || forceCleanup) {
      dropDatabase(project.getName() + FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX, project, dfso);
    }
  }

  public void createDatabase(String dbName, String dbDescription) throws IOException, ServiceException {
    // Create Hive database object
    Database database = new Database();
    database.setName(dbName);
    database.setDescription(dbDescription);

    // Connect to metastore and create the database
    ThriftHiveMetastore.Client client = null;
    try {
      client = openSuperMetastoreClient();
      client.create_database(database);
    } catch (TException e) {
      throw new IOException(e);
    } finally {
      finalizeMetastoreOperation(null, null, client);
    }
  }

  private void dropDatabase(String dbName, Project project, DistributedFileSystemOps dfso)
      throws IOException, ServiceException {
    // Connect to metastore and create the database
    ThriftHiveMetastore.Client superClient = null;
    ThriftHiveMetastore.Client projectOwnerClient = null;

    try {
      projectOwnerClient = openUserMetastoreClient(project, project.getOwner());
      // Drop all hive tables as project owner
      for (String tableName : projectOwnerClient.get_all_tables(dbName)) {
        projectOwnerClient.drop_table(dbName, tableName, true);
      }

      Path dbPath = getDbPath(dbName);
      // User the DFSo to delete the storage_connector_resources
      dfso.rm(new Path(dbPath, FeaturestoreConstants.STORAGE_CONNECTOR_SUBDIR), true);

      // chown the database back to the users
      dfso.setOwner(dbPath, settings.getHiveSuperUser(), settings.getHiveSuperUser());

      // The database is now empty and it can be deleted by the super user
      superClient = openSuperMetastoreClient();
      superClient.drop_database(dbName, true, true);
    } catch (TException e) {
      throw new IOException(e);
    } finally {
      finalizeMetastoreOperation(null, null, superClient);
      finalizeMetastoreOperation(project, project.getOwner(), projectOwnerClient);
    }
  }

  private ThriftHiveMetastore.Client openSuperMetastoreClient() throws ServiceException, IOException {
    return openMetastoreClient(settings.getHopsworksUser(),
        bhcs.getSuperKeystorePath(),
        bhcs.getSuperKeystorePassword(),
        bhcs.getSuperKeystore(),
        bhcs.getSuperTrustStorePath(),
        bhcs.getSuperTrustStorePassword(),
        bhcs.getSuperTrustStore());
  }

  // Here we can't use the HiveMetaStoreClient.java wrapper as we would need to export environment variables and so on
  // instead we assemble directly the thirft client, which is what the HiveMetaStoreClient does behind the scenes.
  public ThriftHiveMetastore.Client openUserMetastoreClient(Project project, Users user)
      throws ServiceException, IOException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    try {
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      CertificateMaterializer.CryptoMaterial userMaterial =
          certificateMaterializer.getUserMaterial(user.getUsername(), project.getName());

      // read Password
      String password = String.copyValueOf(userMaterial.getPassword());

      return openMetastoreClient(hdfsUsername,
          certificateMaterializer.getUserTransientKeystorePath(project, user),
          password,
          userMaterial.getKeyStore(),
          certificateMaterializer.getUserTransientTruststorePath(project, user),
          password,
          userMaterial.getTrustStore()
      );
    } catch (CryptoPasswordNotFoundException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.METASTORE_CONNECTION_ERROR, Level.SEVERE,
          "Hive metastore connection error", e.getMessage(), e);
    }
  }

  private ThriftHiveMetastore.Client openMetastoreClient(String user,
                                                        String keyStorePath,
                                                        String keyStorePassword,
                                                        ByteBuffer keyStore,
                                                        String trustStorePath,
                                                        String trustStorePassword,
                                                        ByteBuffer trustStore) throws ServiceException {
    ThriftHiveMetastore.Client client = null;
    try {
      // Get metastore service information from consul
      Service metastoreService = serviceDiscoveryController
          .getAnyAddressOfServiceWithDNS(HopsworksService.HIVE.getNameWithTag(HiveTags.metastore));

      // Setup secure connection with the Hive metastore.
      TSSLTransportFactory.TSSLTransportParameters params =
          new TSSLTransportFactory.TSSLTransportParameters();
      params.setTrustStore(trustStorePath, trustStorePassword);
      params.setKeyStore(keyStorePath, keyStorePassword);

      TTransport transport = TSSLTransportFactory.getClientSocket(metastoreService.getAddress(),
          metastoreService.getPort(), CONNECTION_TIMEOUT, params);

      TProtocol protocol = new TBinaryProtocol(transport);
      client = new ThriftHiveMetastore.Client(protocol);

      // Open transport
      if (!transport.isOpen()) {
        transport.open();
      }

      // Set the UGI on the metastore side
      client.set_ugi(user, new ArrayList<>());

      // Send the certificate to the metastore so it can operate with the fs.
      client.set_crypto(keyStore, keyStorePassword, trustStore, trustStorePassword, false);
    } catch (ServiceDiscoveryException | TException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.METASTORE_CONNECTION_ERROR, Level.SEVERE,
          "Hive metastore connection error", e.getMessage(), e);
    }

    return client;
  }

  public void finalizeMetastoreOperation(Project project, Users user, ThriftHiveMetastore.Client client) {
    if (project != null && user != null) {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    }

    if (client != null) {
      try {
        client.shutdown();
      } catch (TException e) {
        logger.log(Level.SEVERE, "Error closing Metastore connection", e);
      }
    }
  }

  public Path getDbPath(String dbName) {
    return new Path(settings.getHiveWarehouse(), dbName.toLowerCase() + ".db");
  }

  public String getDbDirName(String dbName) {
    return dbName.toLowerCase() + ".db";
  }

  public String getHiveServerInternalEndpoint() throws ServiceDiscoveryException {
    return getHiveServerEndpoint(HiveTags.hiveserver2_tls);
  }

  private String getHiveServerEndpoint(HiveTags tag)
      throws ServiceDiscoveryException {
    Service hive =
        serviceDiscoveryController.getAnyAddressOfServiceWithDNS(HopsworksService.HIVE.getNameWithTag(tag));
    return hive.getAddress() + ":" + hive.getPort();
  }
}
