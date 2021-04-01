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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the online featurestore databases in Hopsworks and the associated
 * business logic.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OnlineFeaturestoreController {

  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreController.class.getName());
  private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
  private static final String MYSQL_JDBC = "jdbc:mysql://";
  private static final String MYSQL_PROPERTIES = "?useSSL=false&allowPublicKeyRetrieval=true";
  public static final String ONLINEFS_USERNAME = "onlinefs";
  
  @EJB
  private SecretsFacade secretsFacade;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;
  @EJB
  private OnlineFeaturestoreFacade onlineFeaturestoreFacade;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  
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
    try {
      return DriverManager.getConnection(getJdbcURL(databaseName), dbUsername, password);
    } catch (SQLException | ServiceDiscoveryException e) {
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
  public FeaturegroupPreview executeReadJDBCQuery(String query, String databaseName, Project project, Users user)
      throws SQLException, FeaturestoreException {
    Connection conn = null;
    Statement stmt = null;
    try {
      //Re-create the connection every time since the connection is database and user-specific
      conn = initConnection(databaseName, project, user);
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      return cachedFeaturegroupController.parseResultset(rs);
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
  }

  /**
   * Checks if the JDBC connection to MySQL Server is open, and if so closes it.
   *
   * @param conn the JDBC connection
   */
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
   * @param user the project owner
   * @param featurestore the featurestore metadata entity
   * @throws FeaturestoreException
   */
  public void setupOnlineFeaturestore(Users user, Featurestore featurestore)
    throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online feature store service is not enabled for this Hopsworks instance");
    }
    // Create dataset
    addOnlineFeatureStoreDB(getOnlineFeaturestoreDbName(featurestore.getProject()));

    // Create project owner database user
    createDatabaseUser(user, featurestore, ProjectRoleTypes.DATA_OWNER.getRole());
  }
  
  /**
   * Creates a new database user
   *
   * @param user the Hopsworks user
   * @param featurestore the feature store
   * @throws FeaturestoreException
   */
  public void createDatabaseUser(Users user, Featurestore featurestore, String projectRole)
      throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(db)) {
      // There is no online feature store for this feature store
      return;
    }

    String dbUser = onlineDbUsername(featurestore.getProject(), user);
    //Generate random pw
    String onlineFsPw = createOnlineFeaturestoreUserSecret(dbUser, user, featurestore.getProject());
    //database is the same as the project name
    onlineFeaturestoreFacade.createOnlineFeaturestoreUser(dbUser, onlineFsPw);

    updateUserOnlineFeatureStoreDB(user, featurestore, projectRole);
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
   * @param user the user to be added
   * @param featurestore the feature store
   * @throws FeaturestoreException
   */
  public void updateUserOnlineFeatureStoreDB(Users user, Featurestore featurestore, String projectRole)
    throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(db)) {
      //Nothing to update
      return;
    }

    String dbuser = onlineDbUsername(featurestore.getProject(), user);
    onlineFeaturestoreFacade.revokeUserPrivileges(db, dbuser);

    if (projectRole.equals(ProjectRoleTypes.DATA_OWNER.getRole())) {
      onlineFeaturestoreFacade.grantDataOwnerPrivileges(db, dbuser);
    } else {
      onlineFeaturestoreFacade.grantDataScientistPrivileges(db, dbuser);
    }

    try {
      createJdbcConnectorForOnlineFeaturestore(dbuser, featurestore, db);
    } catch(Exception e) {
      //If the connector have already been created, skip this step
    }
  }

  /**
   * Creates a new database and database-user for an online featurestore
   *
   * @param db the database-name
   * @throws FeaturestoreException
   */
  public void addOnlineFeatureStoreDB(String db) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Feature Store is not enabled");
    }

    onlineFeaturestoreFacade.createOnlineFeaturestoreDatabase(db);
  }

  /**
   * Utility function for create a JDBC connection to the online featurestore for a particular user.
   *
   * @param onlineDbUsername the db-username of the connection
   * @param featurestore the featurestore metadata
   * @param dbName name of the MySQL database
   * @return DTO of the newly created connector
   * @throws FeaturestoreException
   */
  public void createJdbcConnectorForOnlineFeaturestore(String onlineDbUsername,
             Featurestore featurestore, String dbName) throws FeaturestoreException {
    String connectorName = onlineDbUsername + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX;
    if (featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName).isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_NAME, Level.FINE,
          "a storage connector with that name already exists");
    }

    FeaturestoreConnector featurestoreConnector = new FeaturestoreConnector();
    featurestoreConnector.setName(connectorName);
    featurestoreConnector.setDescription("JDBC connection to Hopsworks Project Online " +
        "Feature Store NDB Database for user: " + onlineDbUsername);
    featurestoreConnector.setFeaturestore(featurestore);
    featurestoreConnector.setConnectorType(FeaturestoreConnectorType.JDBC);

    FeaturestoreJdbcConnector featurestoreJdbcConnector = new FeaturestoreJdbcConnector();
    featurestoreJdbcConnector.setConnectionString(settings.getFeaturestoreJdbcUrl() + dbName +
        "?useSSL=false&allowPublicKeyRetrieval=true");
    Map<String, String> arguments = new HashMap<>();
    arguments.put(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_PASSWORD_ARG,
        FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_PASSWORD_TEMPLATE);
    arguments.put(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_USER_ARG, onlineDbUsername);
    arguments.put(FeaturestoreConstants.ONLINE_FEATURE_STORE_JDBC_DRIVER_ARG, MYSQL_DRIVER);
    arguments.put("isolationLevel", "NONE");
    arguments.put("batchsize", "500");

    featurestoreJdbcConnector.setArguments(arguments.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(",")));
    featurestoreConnector.setJdbcConnector(featurestoreJdbcConnector);

    featurestoreConnectorFacade.update(featurestoreConnector);
  }
  
  /**
   * Drops an online feature store database and removes all associated users
   *
   * @param project the project of the user making the request
   * @throws FeaturestoreException
   */
  public void removeOnlineFeatureStore(Project project) throws FeaturestoreException {
    if (!settings.isOnlineFeaturestore() || !checkIfDatabaseExists(getOnlineFeaturestoreDbName(project))) {
      //Nothing to remove
      return;
    }
    for (HdfsUsers hdfsUser: hdfsUsersController.getAllProjectHdfsUsers(project.getName())) {
      Users user = userFacade.findByUsername(hdfsUser.getUsername());
      if (user != null) {
        String dbUser = onlineDbUsername(project, user);
        try {
          secretsController.delete(user, dbUser);
        } catch (UserException e) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
                  Level.SEVERE, "Problem removing user-secret to online featurestore");
        }
      }
    }
    String db = getOnlineFeaturestoreDbName(project);
    onlineFeaturestoreFacade.removeOnlineFeaturestoreDatabase(db);

    List<String> users = onlineFeaturestoreFacade.getDatabaseUsers(db);
    for (String dbUser: users) {
      onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser);
    }
  }
  
  public void removeOnlineFeaturestoreUser(Featurestore featurestore, Users user) throws FeaturestoreException {
    String db = getOnlineFeaturestoreDbName(featurestore.getProject());
    if (!checkIfDatabaseExists(db)) {
      //Nothing to remove
      return;
    }

    String dbUser = onlineDbUsername(featurestore.getProject().getName(), user.getUsername());

    SecretId id = new SecretId(user.getUid(), dbUser);
    secretsFacade.deleteSecret(id);
    onlineFeaturestoreFacade.removeOnlineFeaturestoreUser(dbUser);

    featurestoreConnectorFacade.deleteByFeaturestoreName(featurestore,
        dbUser + FeaturestoreConstants.ONLINE_FEATURE_STORE_CONNECTOR_SUFFIX);
  }

  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param featurestore the feature store for which to compute the online size
   * @return the size in MB
   */
  public Double getDbSize(Featurestore featurestore) {
    String onlineName = getOnlineFeaturestoreDbName(featurestore.getProject());
    return onlineFeaturestoreFacade.getDbSize(onlineName);
  }

  /**
   * Generate random user password for the online featurestore.
   *
   * @return String password
   */
  private String generateRandomUserPw() {
    return RandomStringUtils.randomAlphabetic(FeaturestoreConstants.ONLINE_FEATURESTORE_PW_LENGTH);
  }

  /**
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  public Boolean checkIfDatabaseExists(String dbName) {
    return onlineFeaturestoreFacade.checkIfDatabaseExists(dbName);
  }

  public String getJdbcURL() throws ServiceDiscoveryException {
    return getJdbcURL("");
  }

  private String getJdbcURL(String dbName) throws ServiceDiscoveryException {
    return MYSQL_JDBC + serviceDiscoveryController
        .constructServiceAddressWithPort(ServiceDiscoveryController.HopsworksService.ONLINEFS_MYSQL)
        + "/" + dbName + MYSQL_PROPERTIES;
  }
}
