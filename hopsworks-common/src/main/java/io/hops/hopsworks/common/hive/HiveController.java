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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featorestore.FeaturestoreConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless(name = "HiveController")
public class HiveController {

  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private BaseHadoopClientsService bhcs;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ActivityFacade activityFacade;

  private final static String driver = "org.apache.hive.jdbc.HiveDriver";
  private final static Logger logger = Logger.getLogger(HiveController.class.getName());

  private Connection conn;
  private String jdbcString = null;
  
  @PostConstruct
  public void init() {
    try {
      // Load Hive JDBC Driver
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      logger.log(Level.SEVERE, "Could not load the Hive driver: " + driver, e);
    }
  }

  private void initConnection() throws SQLException {
    // Create connection url
    String hiveEndpoint = settings.getHiveServerHostName(false);
    jdbcString = "jdbc:hive2://" + hiveEndpoint + "/default;" +
      "auth=noSasl;ssl=true;twoWay=true;" +
      "sslTrustStore=" + bhcs.getSuperTrustStorePath() + ";" +
      "trustStorePassword=" + bhcs.getSuperTrustStorePassword() + ";" +
      "sslKeyStore=" + bhcs.getSuperKeystorePath() + ";" +
      "keyStorePassword=" + bhcs.getSuperKeystorePassword();
  
    conn = DriverManager.getConnection(jdbcString);
  }

  @PreDestroy
  public void close() {
    try {
      if (conn != null && !conn.isClosed()) {
        conn.close();
      }
    } catch (SQLException e) {
      logger.log(Level.WARNING, "Error closing Hive JDBC connection: " +
          e);
    }
  }

  /**
   * Creates a Hopsworks dataset of a Hive database
   *
   * @param project the project of the hive database and the place where the dataset will reside
   * @param user the user making the request
   * @param dfso dfso
   * @param dbName name of the hive database
   * @throws IOException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDatasetDb(Project project, Users user, DistributedFileSystemOps dfso,
                              String dbName) throws IOException {
    createDatasetDb(project, user, dfso, dbName, DatasetType.HIVEDB, null);
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDatasetDb(Project project, Users user, DistributedFileSystemOps dfso,
                               String dbName, DatasetType datasetType, Featurestore featurestore) throws IOException {
    if(datasetType != DatasetType.HIVEDB && datasetType != DatasetType.FEATURESTORE) {
      throw new IllegalArgumentException("Invalid dataset type for hive database");
    }

    // Hive database names are case insensitive and lower case
    Path dbPath = getDbPath(dbName);
    Inode dbInode = inodeFacade.getInodeAtPath(dbPath.toString());

    // Persist Hive db as dataset in the Hopsworks database
    Dataset dbDataset = new Dataset(dbInode, project);
    dbDataset.setType(datasetType);
    dbDataset.setSearchable(true);
    dbDataset.setFeaturestore(featurestore);
    datasetFacade.persistDataset(dbDataset);

    dfso.setMetaEnabled(dbPath);
    datasetController.logDataset(dbDataset, OperationType.Add);
  
    activityFacade.persistActivity(ActivityFacade.NEW_DATA + dbDataset.getName(), project, user, ActivityFlag.DATASET);

    try {
      // Assign database directory to the user and project group
      hdfsUsersBean.addDatasetUsersGroups(user, project, dbDataset, dfso);

      // Make the dataset editable by default
      final FsPermission fsPermission = FsPermissions.rwxrwx___T;
      dfso.setPermission(dbPath, fsPermission);

      // Set the default quota
      switch (datasetType) {
        case HIVEDB:
          dfso.setHdfsSpaceQuotaInMBs(dbPath, settings.getHiveDbDefaultQuota());
          break;
        case FEATURESTORE:
          dfso.setHdfsSpaceQuotaInMBs(dbPath, settings.getFeaturestoreDbDefaultQuota());
          break;
      }
      projectFacade.setTimestampQuotaUpdate(project, new Date());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot assign Hive database directory " + dbPath.toString() +
          " to correct user/group. Trace: " + e);

      // Remove the database directory and cleanup the metadata
      try {
        dfso.rm(dbPath, true);
      } catch (IOException rmEx) {
        // Nothing we can really do here
        logger.log(Level.SEVERE, "Cannot delete Hive database directory: " + dbPath.toString() +
            " Trace: " + rmEx);
      }
      throw new IOException(e);
    }
  }

  /**
   * Creates the Hive Database
   *
   * @param dbName name of the database
   * @param dbComment description of the database
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDatabase(String dbName, String dbComment)
      throws SQLException {
    if (conn == null || conn.isClosed()) {
      initConnection();
    }

    Statement stmt = null;
    try {
      // Create database
      stmt = conn.createStatement();
      // Project name cannot include any spacial character or space.
      stmt.executeUpdate("create database " + dbName + " COMMENT '" + dbComment + "'");
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
  }

  public void dropDatabases(Project project, DistributedFileSystemOps dfso, boolean forceCleanup)
      throws IOException {
    // To avoid case sensitive bugs, check if the project has a Hive database
    Dataset projectDs = datasetController
      .getByProjectAndDsName(project, this.settings.getHiveWarehouse(), project.getName().toLowerCase() + ".db");
    Dataset featurestoreDs = datasetController.getByProjectAndDsName(project, this.settings.getHiveWarehouse(),
      project.getName().toLowerCase() + FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX + ".db");
    if ((projectDs != null && projectDs.getType() == DatasetType.HIVEDB)
        || forceCleanup) {
      dropDatabase(project, dfso, project.getName());
    }

    if ((featurestoreDs != null && featurestoreDs.getType() == DatasetType.FEATURESTORE)
        || forceCleanup) {
      dropDatabase(project, dfso, project.getName() + FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX);
    }
  }

  private void dropDatabase(Project project, DistributedFileSystemOps dfso, String dbName) throws IOException {
    // Delete HopsFs db directory -- will automatically clean up all the related Hive's metadata
    dfso.rm(getDbPath(dbName), true);
    // Delete all the scratchdirs
    for (HdfsUsers u : hdfsUsersBean.getAllProjectHdfsUsers(project.getName())) {
      dfso.rm(new Path(settings.getHiveScratchdir(), u.getName()), true);
    }
  }

  public Path getDbPath(String dbName) {
    return new Path(settings.getHiveWarehouse(), dbName.toLowerCase() + ".db");
  }

}
