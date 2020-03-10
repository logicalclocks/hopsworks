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
package io.hops.hopsworks.common.featurestore.online;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.RowValueQueryResult;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online.OnlineFeaturegroup;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.SecretId;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the online featurestore databases in Hopsworks and the associated
 * business logic.
 */
@Stateless
public class OnlineFeaturestoreController {

  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreController.class.getName());
  private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
  
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private FeaturestoreJdbcConnectorController featurestoreJdbcConnectorController;
  @EJB
  private OnlineFeaturestoreFacade onlineFeaturestoreFacade;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;
  
  @PostConstruct
  public void init() {
    try {
      // Load MySQL JDBC Driver
      Class.forName(MYSQL_DRIVER);
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Could not load the MySQL JDBC driver: " + MYSQL_DRIVER, e);
    }
  }
  
  /**
   * Initializes a JDBC connection MySQL Server using an online featurestore user and password
   *
   * @param databaseName name of the MySQL database to open a connection to
   * @param project      the project of the user making the request
   * @param user         the user making the request
   * @return conn the JDBC connection
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private Connection initConnection(String databaseName, Project project, Users user) throws FeaturestoreException {
    String jdbcString = "";
    String dbUsername = onlineDbUsername(project, user);
    String password = "";
    try {
      password = secretsController.get(user, dbUsername).getPlaintext();
    } catch (UserException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
        Level.SEVERE, "Problem getting secrets for the JDBC connection to the online FS");
    }
    jdbcString = settings.getFeaturestoreJdbcUrl() + databaseName;
    try {
      return DriverManager.getConnection(jdbcString, dbUsername, password);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE,
        "Error initiating MySQL JDBC connection to online feature store for user: " + user.getEmail() + " error:"
          + e);
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE, Level.SEVERE,
        "project: " + project.getName() + ", database: " + databaseName + ", db user:" + dbUsername +
                ", jdbcString: " + jdbcString, e.getMessage(), e);
    }
  }
  
  /**
   * Runs a update/create SQL query against an online featurestore database, impersonating the user making the request
   *
   * @param query the update/create query to run
   * @param databaseName the name of the database to run the query against
   * @param project the project of the online featurestore
   * @param user the user to run the query as
   * @throws FeaturestoreException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void executeUpdateJDBCQuery(String query, String databaseName, Project project, Users user)
    throws FeaturestoreException, SQLException {
    //Re-create the connection every time since the connection is database and user-specific
    Statement stmt = null;
    Connection conn = null;
    //Run Query
    try {
      conn = initConnection(databaseName, project, user);
      stmt = conn.createStatement();
      stmt.executeUpdate(query);
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MYSQL_JDBC_UPDATE_STATEMENT_ERROR, Level.SEVERE,
        "project: " + project.getName() + ", Online featurestore database: " + databaseName + " jdbc query: " + query,
        e.getMessage(), e);
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      closeConnection(conn);
    }
  }
  
  /**
   * Runs a Read-SQL query against an online featurestore database, impersonating the user making the request
   *
   * @param query        the read query
   * @param databaseName the name of the MySQL database
   * @param project      the project that owns the online featurestore
   * @param user         the user making the request
   * @return parsed resultset
   * @throws SQLException
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<RowValueQueryResult> executeReadJDBCQuery(
    String query, String databaseName, Project project, Users user)
    throws SQLException, FeaturestoreException {
    Connection conn = null;
    Statement stmt = null;
    List<RowValueQueryResult> resultList = null;
    try {
      //Re-create the connection every time since the connection is database and user-specific
      conn = initConnection(databaseName, project, user);
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      resultList = cachedFeaturegroupController.parseResultset(rs);
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MYSQL_JDBC_READ_QUERY_ERROR, Level.SEVERE,
        "project: " + project.getName() + ", mysql database: " + databaseName + " jdbc query: " + query,
        e.getMessage(), e);
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      closeConnection(conn);
    }
    return resultList;
  }
  
  /**
   * Queries the metadata in MySQL-Cluster to get the schema information of an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup to get type information for
   * @return a list of Feature DTOs with the type information
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<FeatureDTO> getOnlineFeaturegroupFeatures(OnlineFeaturegroup onlineFeaturegroup) {
    return onlineFeaturestoreFacade.getMySQLFeatures(onlineFeaturegroup.getTableName(),
        onlineFeaturegroup.getDbName().toLowerCase());
  }
  
  /**
   * Checks if the JDBC connection to MySQL Server is open, and if so closes it.
   *
   * @param conn the JDBC connection
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void closeConnection(Connection conn) {
    try {
      if (conn != null) {
        conn.close();
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Error closing MySQL JDBC connection: " +  e);
    }
  }
  
  /**
   * Sets up the online feature store database for a new project and creating a database-user for the project-owner
   *
   * @param project the project that should own the online featurestore database
   * @param user the project owner
   * @param featurestore the featurestore metadata entity
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void setupOnlineFeaturestore(Project project, Users user, Featurestore featurestore)
    throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online feature store service is not enabled for this Hopsworks instance");
    }
    /*
     * Step 1: Create Database
     */
    addOnlineFeatureStoreDB(getOnlineFeaturestoreDbName(project));
    /*
     * Step 2: Create database user
     */
    createDatabaseUser(user, project);
    /*
     * Step 3: Grant user privileges
     */
    updateUserOnlineFeatureStoreDB(project, user, featurestore);
  }
  
  /**
   * Creates a new database user
   *
   * @param user the Hopsworks user
   * @param project the project of the Hopsworks user
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDatabaseUser(Users user, Project project) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Feature Store is not enabled");
    }
    String dbUser = onlineDbUsername(project, user);
    //Generate random pw
    String onlineFsPw = createOnlineFeaturestoreUserSecret(dbUser, user, project);
    //database is the same as the project name
    try {
      onlineFeaturestoreFacade.createOnlineFeaturestoreUser(dbUser, onlineFsPw);
    } catch (Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_USER,
        Level.SEVERE, "An error occurred when trying to create the MySQL database user for an online feature store",
        e.getMessage(), e);
    }
  }
  
  /**
   * Stores the online-featurestore password as a Hopsworks secret for the user
   *
   * @param dbuser the database-user
   * @param user the user to store the secret for
   * @param project the project of the online feature store
   * @return the password
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private String createOnlineFeaturestoreUserSecret(String dbuser, Users user, Project project)
    throws FeaturestoreException {
    String onlineFsPw = generateRandomUserPw();
    try {
      secretsController.delete(user, dbuser); //Delete if the secret already exsits
      secretsController.add(user, dbuser, onlineFsPw, VisibilityType.PRIVATE, project.getId());
    } catch (UserException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
              Level.SEVERE, "Problem adding online featurestore password to hopsworks secretsmgr");
    }
    return onlineFsPw;
  }
  
  /**
   * Gets the name of the online database given a project and a user
   *
   * @param project the project
   * @param user the user
   * @return a string representing the online database name
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String onlineDbUsername(Project project, Users user) {
    return onlineDbUsername(project.getName(), user.getUsername());
  }
  
  /**
   * Gets the featurestore MySQL DB name of a project
   *
   * @param project the project to get the MySQL-db name of the feature store for
   * @return the MySQL database name of the featurestore in the project
   */
  public String getOnlineFeaturestoreDbName(Project project) {
    return project.getName().toLowerCase();
  }

  /**
   * The mysql username can be at most 32 characters in length. 
   * Clip the username to 32 chars if it is longer than 32.
   * 
   * @param project projectname
   * @param user username
   * @return the mysql username for the online featuer store
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private String onlineDbUsername(String project, String user) {
    String username = project + "_" + user;
    if (username.length() > FeaturestoreConstants.ONLINE_FEATURESTORE_USERNAME_MAX_LENGTH) {
      username = username.substring(0, FeaturestoreConstants.ONLINE_FEATURESTORE_USERNAME_MAX_LENGTH-1);
    }
    return username;
  }
  
  /**
   * Updates the user-privileges on the online feature store database
   *
   * @param project the project of the user
   * @param user the user to be added
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateUserOnlineFeatureStoreDB(Project project, Users user, Featurestore featurestore)
    throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(getOnlineFeaturestoreDbName(project))) {
      //Nothing to update
      return;
    }
    String dbuser = onlineDbUsername(project, user);
    String db = getOnlineFeaturestoreDbName(project);
    onlineFeaturestoreFacade.revokeUserPrivileges(db, dbuser);
    try{
      if (projectTeamFacade.findCurrentRole(project, user).equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
        onlineFeaturestoreFacade.grantDataOwnerPrivileges(db, dbuser);
      } else {
        onlineFeaturestoreFacade.grantDataScientistPrivileges(db, dbuser);
      }
      try{
        featurestoreJdbcConnectorController.createJdbcConnectorForOnlineFeaturestore(dbuser, featurestore, db);
      } catch(Exception e) {
        //If the connector have already been created, skip this step
      }
    } catch(Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode
        .ERROR_GRANTING_ONLINE_FEATURESTORE_USER_PRIVILEGES, Level.SEVERE, "An error occurred when trying to create " +
        "the MySQL database for an online feature store", e.getMessage(), e);
    }
  }

  /**
   * Creates a new database and database-user for an online featurestore
   *
   * @param db the database-name
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void addOnlineFeatureStoreDB(String db) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Feature Store is not enabled");
    }
    try {
      onlineFeaturestoreFacade.createOnlineFeaturestoreDatabase(db);
    } catch (Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_DB,
        Level.SEVERE, "An error occurred when trying to create the MySQL database for an online feature store",
        e.getMessage(), e);
    }
  }
  
  /**
   * Drops an online feature store database and removes all associated users
   *
   * @param project the project of the user making the request
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeOnlineFeatureStore(Project project) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(getOnlineFeaturestoreDbName(project))) {
      //Nothing to remove
      return;
    }
    for (HdfsUsers hdfsUser: hdfsUsersController.getAllProjectHdfsUsers(project.getName())) {
      Users user = userFacade.findByUsername(hdfsUser.getUsername());
      String dbUser = onlineDbUsername(project, user);
      try {
        secretsController.delete(user, dbUser);
      } catch (UserException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
                Level.SEVERE, "Problem removing user-secret to online featurestore");
      }
    }
    String db = getOnlineFeaturestoreDbName(project);
    try {
      onlineFeaturestoreFacade.removeOnlineFeaturestoreDatabase(db);
    } catch (Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ONLINE_FEATURESTORE_DB,
        Level.SEVERE, "An error occurred when trying to delete the MySQL database user for an online feature store",
        e.getMessage(), e);
    }
    List<String> users = onlineFeaturestoreFacade.getDatabaseUsers(db);
    for (String dbUser: users) {
      try{
        onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser);
      } catch(Exception e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ONLINE_FEATURESTORE_USER,
          Level.SEVERE, "An error occurred when trying to delete the MySQL database user for an online feature store",
          e.getMessage(), e);
      }
    }
  }
  
  /**
   * Removes a user from a online feature store database in the project
   *
   * @param project the project that owns the online feature store
   * @param user the user to remove
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeOnlineFeaturestoreUser(Project project, Users user) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      //Nothing to remove
      return;
    }
    String dbUser = onlineDbUsername(project.getName(), user.getUsername());
    SecretId id = new SecretId(user.getUid(), dbUser);
    secretsFacade.deleteSecret(id);
    try {
      onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser);
    } catch (Exception e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ONLINE_FEATURESTORE_USER,
        Level.SEVERE, "An error occurred when trying to delete the MySQL database user for an online feature store",
        e.getMessage(), e);
    }
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithName(project,
      featurestoreController.getOfflineFeaturestoreDbName(project));
    String connectorName = dbUser + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    List<FeaturestoreStorageConnectorDTO> jdbcConnectors =
      featurestoreJdbcConnectorController.getJdbcConnectorsForFeaturestore(
        featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId()));
    for (FeaturestoreStorageConnectorDTO storageConnector: jdbcConnectors) {
      if (storageConnector.getName().equalsIgnoreCase(connectorName)) {
        featurestoreJdbcConnectorController.removeFeaturestoreJdbcConnector(storageConnector.getId());
      }
    }
  }
  
  
  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @return the size in MB
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Double getDbSize(String dbName) {
    Double dbSize = onlineFeaturestoreFacade.getDbSize(dbName);
    if(dbSize == null){
      return 0.0;
    }
    return dbSize;
  }
  
  /**
   * Gets the size of an online featuregroup table (MB).
   *
   * @param onlineFeaturegroupDTO metadata about the online featuregroup
   * @return the size in MB
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Double getTblSize(OnlineFeaturegroupDTO onlineFeaturegroupDTO) {
    return onlineFeaturestoreFacade.getTblSize(onlineFeaturegroupDTO.getTableName(),
        onlineFeaturegroupDTO.getDbName().toLowerCase());
  }
  
  /**
   * Gets the SQL schema of an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup to get the SQL schema of
   * @return a String with the "SHOW CREATE TABLE" result
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String getOnlineFeaturegroupSchema(OnlineFeaturegroupDTO onlineFeaturegroup) {
    return onlineFeaturestoreFacade.getMySQLSchema(onlineFeaturegroup.getTableName(),
        onlineFeaturegroup.getDbName().toLowerCase());
  }
  
  /**
   * Gets a preview of an online feature group (select * from tbl LIMIT 20)
   *
   * @param onlineFeaturegroup the online featuregroup to preview
   * @return a preview of 20 rows in the table
   * @throws FeaturestoreException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getOnlineFeaturegroupPreview(OnlineFeaturegroupDTO onlineFeaturegroup, Users user,
    Featurestore featurestore ) throws FeaturestoreException, SQLException {
    String tbl = onlineFeaturegroup.getTableName();
    String query = "SELECT * FROM " + tbl + " LIMIT 20";
    String db = onlineFeaturegroup.getDbName().toLowerCase();
    try {
      return executeReadJDBCQuery(query, db, featurestore.getProject(), user);
    } catch(Exception e) {
      return executeReadJDBCQuery(query, db, featurestore.getProject(), user);
    }
  }
  
  /**
   * Gets the table type of an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup to get the table type of
   * @return the table type
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String getOnlineFeaturegroupTableType(OnlineFeaturegroupDTO onlineFeaturegroup) {
    return onlineFeaturestoreFacade.getMySQLTableType(onlineFeaturegroup.getTableName(),
      onlineFeaturegroup.getDbName().toLowerCase());
  }
  
  /**
   * Gets the number of rows in an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup to get the number of rows of
   * @return the number of rows in the table
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Integer getOnlineFeaturegroupTableRows(OnlineFeaturegroupDTO onlineFeaturegroup) {
    return onlineFeaturestoreFacade.getMySQLTableRows(onlineFeaturegroup.getTableName(),
      onlineFeaturegroup.getDbName().toLowerCase());
  }
  
  /**
   * Generate random user password for the online featurestore.
   *
   * @return String password
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private String generateRandomUserPw() {
    return RandomStringUtils.randomAlphabetic(FeaturestoreConstants.ONLINE_FEATURESTORE_PW_LENGTH);
  }
  
  
  /**
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Boolean checkIfDatabaseExists(String dbName) {
    return onlineFeaturestoreFacade.checkIfDatabaseExists(dbName);
  }
  
}
