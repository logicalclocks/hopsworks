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
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.MysqlTags;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the online feature store databases (separate from the Hopsworks databases).
 * This interface is supposed to be used for any DDL queries on online feature store databases. DML and analytic
 * queries are done through JDBC and client-libraries.
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OnlineFeaturestoreFacade {

  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreFacade.class.getName());

  public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
  public static final String MYSQL_JDBC = "jdbc:mysql://";
  public static final String MYSQL_PROPERTIES = "?useSSL=false&allowPublicKeyRetrieval=true";

  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private Settings settings;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private SecretsController secretsController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;

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
   * Create an Online Featurestore Database. Fails if the database already exists.
   *
   * @param db name of the database
   */
  public void createOnlineFeaturestoreDatabase(String db, Connection connection) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    //Don't add 'IF EXISTS', this call should fail if the database already exists
    try {
      executeUpdate("CREATE DATABASE " + db + ";", connection);
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_DB,
          Level.SEVERE, "Error running create query", se.getMessage(), se);
    }
  }

  /**
   * Removes an Online Featurestore Database
   *
   * @param db name of the table
   */
  public void removeOnlineFeaturestoreDatabase(String db, Connection connection) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      executeUpdate("DROP DATABASE IF EXISTS " + db + ";", connection);
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ONLINE_FEATURESTORE_DB,
          Level.SEVERE, "Error running drop query", se.getMessage(), se);
    }
  }

  /**
   * Create an Online Featurestore Databasse User
   *
   * @param user the database username
   * @param pw the database user password
   */
  public void createOnlineFeaturestoreUser(String user, String pw, Connection connection) throws FeaturestoreException {
    try {
      try (PreparedStatement pStmt = connection.prepareStatement("CREATE USER IF NOT EXISTS ? IDENTIFIED BY ?;");
           Statement stmt = connection.createStatement()) {
        pStmt.setString(1, user);
        pStmt.setString(2, pw);
        pStmt.executeUpdate();
        stmt.executeUpdate("GRANT NDB_STORED_USER ON *.* TO " + user + ";");
      }
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_USER,
          Level.SEVERE, "Error occurred when trying to create the MySQL database user for an online feature store",
          se.getMessage(), se);
    }
  }

  /**
   * Removes a database user for an online featurestore
   *
   * @param dbUser the database-username
   */
  public void removeOnlineFeaturestoreUser(String dbUser, Connection connection) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      try (PreparedStatement pStmt = connection.prepareStatement("DROP USER IF EXISTS ?")) {
        pStmt.setString(1, dbUser);
        pStmt.executeUpdate();
      }
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_ONLINE_FEATURESTORE_USER,
          Level.SEVERE, "An error occurred when trying to delete the MySQL database user for an online feature store",
          se.getMessage(), se);
    }
  }

  /**
   * Grant database privileges of a "data owner" role in a online featurestore
   *
   * @param dbName name of the online featurestore database
   * @param dbUser the database-username
   */
  public void grantDataOwnerPrivileges(String dbName, String dbUser, Connection conn) throws FeaturestoreException {
    try {
      grantUserPrivileges(dbUser, "GRANT ALL PRIVILEGES ON " + dbName + ".* TO " + dbUser + ";", dbName,
        conn);
    } catch (SQLException se) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ERROR_GRANTING_ONLINE_FEATURESTORE_USER_PRIVILEGES, Level.SEVERE,
          "Error running the grant query", se.getMessage(), se);
    }
  }

  /**
   * Grant database privileges of a "data scientist" role in a online featurestore
   *
   * @param dbName name of the online featurestore database
   * @param dbUser the database-username
   */
  public void grantDataScientistPrivileges(String dbName, String dbUser, Connection conn) throws FeaturestoreException {
    try {
      grantUserPrivileges(dbUser, "GRANT SELECT ON " + dbName + ".* TO " + dbUser + ";", dbName, conn);
    } catch (SQLException se) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ERROR_GRANTING_ONLINE_FEATURESTORE_USER_PRIVILEGES, Level.SEVERE,
          "Error running the grant query", se.getMessage(), se);
    }
  }

  private void grantUserPrivileges(String dbUser, String grantQuery, String dbName, Connection conn)
    throws FeaturestoreException,
      SQLException {
    ResultSet resultSet = null;
    try (PreparedStatement pStmt = conn.prepareStatement(
             "SELECT COUNT(*) FROM mysql.user WHERE User = ?")){
      // If the user doesn't exist, the grant permissions will fail blocking the rest of the user assignments
      // check if the user exists before executing the granting command
      pStmt.setString(1, dbUser);
      resultSet = pStmt.executeQuery();
  
      if (resultSet.next() && resultSet.getInt(1) != 0) {
        revokeUserPrivileges(dbName, dbUser, conn);
        executeUpdate(grantQuery, conn);
      }
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }
  }

  /**
   * Gets the features of a online featuregroup from the MySQL metadata
   *
   * @param tableName the name of the table of the online featuregroup
   * @param db the name of the mysql database
   * @return list of featureDTOs with name,type,comment
   */
  public List<FeatureGroupFeatureDTO> getMySQLFeatures(String tableName, String db) throws FeaturestoreException {
    ArrayList<FeatureGroupFeatureDTO> featureGroupFeatureDTOS = new ArrayList<>();
    try {
      ResultSet resultSet = null;
      try (Connection connection = establishAdminConnection();
           PreparedStatement pStmt = connection.prepareStatement(
               "SELECT `COLUMNS`.`COLUMN_NAME`,`COLUMNS`.`COLUMN_TYPE`, `COLUMNS`.`COLUMN_COMMENT` " +
               "FROM INFORMATION_SCHEMA.`COLUMNS` " +
               "WHERE `COLUMNS`.`TABLE_NAME`=? AND `COLUMNS`.`TABLE_SCHEMA`=?;")) {
        pStmt.setString(1, tableName);
        pStmt.setString(2, db);
        resultSet = pStmt.executeQuery();
        while (resultSet.next()) {
          featureGroupFeatureDTOS.add(new FeatureGroupFeatureDTO(resultSet.getString(1),
              resultSet.getString(2),
              resultSet.getString(3)));
        }
      } finally {
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_ONLINE_FEATURES, Level.SEVERE,
          "Error reading features from schema", se.getMessage(), se);
    }

    return featureGroupFeatureDTOS;
  }
  
  public void revokeUserPrivileges(String dbName, String dbUser, Connection connection) {
    ResultSet resultSet = null;
    try {
      try (PreparedStatement pStmt = connection.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ?")){
        // If the grant does not exists, MySQL returns a 1141 error which JPA catches and logs it together
        // with the stack trace, polluting the logs. To avoid this we first query the information_schema
        // to check that the grant exists, if so, we remove it
        String grantee = "'" + dbUser + "'@'%'";
        pStmt.setString(1, grantee);
        pStmt.setString(2, dbName);
        
        resultSet = pStmt.executeQuery();
        if (resultSet.next() && resultSet.getInt(1) != 0) {
          //Prepared statements with parameters can only be done for
          //WHERE/HAVING Clauses, not names of tables or databases
          executeUpdate("REVOKE ALL PRIVILEGES ON " + dbName + ".* FROM " + dbUser + ";", connection);
        }
      } finally {
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception in revoking the privileges", e);
    }
  }


  /**
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  public Boolean checkIfDatabaseExists(String dbName) {
    try {
      ResultSet resultSet = null;
      try (Connection connection = establishAdminConnection();
           PreparedStatement pStmt = connection.prepareStatement(
               "SELECT `SCHEMA_NAME` FROM `INFORMATION_SCHEMA`.`SCHEMATA` WHERE `SCHEMA_NAME`=?")) {
        pStmt.setString(1, dbName);
        resultSet = pStmt.executeQuery();
        return resultSet.next();
      } finally {
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (SQLException | FeaturestoreException se) {
      LOGGER.log(Level.SEVERE, "Error checking if database exists", se);
      return false;
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
      throws FeaturestoreException{
    //Re-create the connection every time since the connection is database and user-specific
    //Run Query
    try (Connection conn = establishUserConnection(databaseName, project, user);
         Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(query);
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MYSQL_JDBC_UPDATE_STATEMENT_ERROR, Level.SEVERE,
          "project: " + project.getName() + ", Online featurestore database: " + databaseName + " jdbc query: " + query,
          e.getMessage(), e);
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
      throws FeaturestoreException {
    try (Connection conn = establishUserConnection(databaseName, project, user);
         Statement stmt = conn.createStatement()) {
      //Re-create the connection every time since the connection is database and user-specific
      ResultSet rs = stmt.executeQuery(query);
      return featurestoreUtils.parseResultset(rs);
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MYSQL_JDBC_READ_QUERY_ERROR, Level.SEVERE,
          "project: " + project.getName() + ", mysql database: " + databaseName + " jdbc query: " + query,
          e.getMessage(), e);
    }
  }

  /**
   * Create a Kafka Offset table in Online Featurestore Database.
   *
   * @param db name of the database
   */
  public void createOnlineFeaturestoreKafkaOffsetTable(String db, Connection connection) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      executeUpdate(
              "CREATE TABLE IF NOT EXISTS `" + db + "`.`kafka_offsets` (\n" +
                      "`topic` varchar(255) COLLATE latin1_general_cs NOT NULL,\n" +
                      "`partition` SMALLINT NOT NULL,\n" +
                      "`offset` BIGINT UNSIGNED NOT NULL,\n" +
                      "PRIMARY KEY (`topic`,`partition`)\n" +
                    ") ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;", connection);
    } catch (SQLException se) {
      throw new FeaturestoreException(
              RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_KAFKA_OFFSET_TABLE,
              Level.SEVERE, "Error running create query", se.getMessage(), se);
    }
  }

  private void executeUpdate(String query, Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate(query);
    }
  }

  public Connection establishAdminConnection() throws FeaturestoreException {
    try {
      return DriverManager.getConnection(getJdbcURL(),
          settings.getVariableFeaturestoreDbAdminUser(),
          settings.getVariableFeaturestoreDbAdminPwd());
    } catch (SQLException | ServiceDiscoveryException e) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE,
          Level.SEVERE, e.getMessage(), e.getMessage(), e);
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
  private Connection establishUserConnection(String databaseName, Project project, Users user)
      throws FeaturestoreException {
    String dbUsername = onlineFeaturestoreController.onlineDbUsername(project, user);
    String password;
    try {
      password = secretsController.get(user, dbUsername).getPlaintext();
    } catch (UserException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_SECRETS_ERROR,
          Level.SEVERE, "Problem getting secrets for the JDBC connection to the online FS");
    }

    String jdbcString = "";
    try {
      jdbcString = getJdbcURL(databaseName);
      return DriverManager.getConnection(jdbcString, dbUsername, password);
    } catch (SQLException | ServiceDiscoveryException e) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_MYSQL_CONNECTION_TO_ONLINE_FEATURESTORE, Level.SEVERE,
          "project: " + project.getName() + ", database: " + databaseName + ", db user:" + dbUsername +
              ", jdbcString: " + jdbcString, e.getMessage(), e);
    }
  }

  public String getJdbcURL() throws ServiceDiscoveryException {
    return getJdbcURL("");
  }

  public String getJdbcURL(String dbName) throws ServiceDiscoveryException {
    return MYSQL_JDBC + serviceDiscoveryController
        .constructServiceAddressWithPort(HopsworksService.MYSQL.getNameWithTag(MysqlTags.onlinefs))
        + "/" + dbName + MYSQL_PROPERTIES;
  }
}
