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

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.Connection;
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

  @Resource(name = "jdbc/featurestore")
  private DataSource featureStoreDataSource;

  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @return the size in MB
   */
  public Double getDbSize(String dbName) {
    try {
      ResultSet resultSet = null;
      try (Connection connection = featureStoreDataSource.getConnection();
           PreparedStatement pStmt = connection.prepareStatement("SELECT " +
               "ROUND(SUM(`tables`.`data_length` + `index_length`) / 1024 / 1024, 1) AS 'size_mb' " +
               "FROM information_schema.`tables` " +
               "WHERE `tables`.`table_schema`=? GROUP BY `tables`.`table_schema`")) {
        pStmt.setString(1, dbName);
        resultSet = pStmt.executeQuery();
        if (resultSet.next()) {
          return resultSet.getDouble("size_mb");
        }
      } finally {
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (SQLException se) {
      LOGGER.log(Level.SEVERE, "Could not get database size", se);
    }

    return 0.0;
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
      try (Connection connection = featureStoreDataSource.getConnection();
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

  /**
   * Create an Online Featurestore Database. Fails if the database already exists.
   *
   * @param db name of the database
   */
  public void createOnlineFeaturestoreDatabase(String db) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    //Don't add 'IF EXISTS', this call should fail if the database already exists
    try {
      executeUpdate("CREATE DATABASE " + db + ";");
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
  public void removeOnlineFeaturestoreDatabase(String db) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      executeUpdate("DROP DATABASE IF EXISTS " + db + ";");
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
  public void createOnlineFeaturestoreUser(String user, String pw) throws FeaturestoreException {
    try {
      try (Connection connection = featureStoreDataSource.getConnection();
           PreparedStatement pStmt = connection.prepareStatement("CREATE USER IF NOT EXISTS ? IDENTIFIED BY ?;")) {
        pStmt.setString(1, user);
        pStmt.setString(2, pw);
        pStmt.executeUpdate();
      }
      executeUpdate("GRANT NDB_STORED_USER ON *.* TO " + user + ";");
    } catch (SQLException se) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_USER,
          Level.SEVERE, "Error occurred when trying to create the MySQL database user for an online feature store",
          se.getMessage(), se);
    }
  }

  /**
   * Revokes user privileges for a user on a specific online featurestore
   *
   * @param dbName name of the MYSQL database
   * @param dbUser the database username to revoke privileges for
   */
  public void revokeUserPrivileges(String dbName, String dbUser) {
    ResultSet resultSet = null;
    try {
      try (Connection connection = featureStoreDataSource.getConnection();
         PreparedStatement pStmt = connection.prepareStatement(
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
          executeUpdate("REVOKE ALL PRIVILEGES ON " + dbName + ".* FROM " + dbUser + ";");
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
   * Grant database privileges of a "data owner" role in a online featurestore
   *
   * @param dbName name of the online featurestore database
   * @param dbUser the database-username
   */
  public void grantDataOwnerPrivileges(String dbName, String dbUser) throws FeaturestoreException {
    try {
      grantUserPrivileges(dbUser, "GRANT ALL PRIVILEGES ON " + dbName + ".* TO " + dbUser + ";");
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
  public void grantDataScientistPrivileges(String dbName, String dbUser) throws FeaturestoreException {
    try {
      grantUserPrivileges(dbUser, "GRANT SELECT ON " + dbName + ".* TO " + dbUser + ";");
    } catch (SQLException se) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ERROR_GRANTING_ONLINE_FEATURESTORE_USER_PRIVILEGES, Level.SEVERE,
          "Error running the grant query", se.getMessage(), se);
    }
  }

  private void grantUserPrivileges(String dbUser, String grantQuery) throws SQLException {
    ResultSet resultSet = null;
    try (Connection connection = featureStoreDataSource.getConnection();
         PreparedStatement pStmt = connection.prepareStatement(
             "SELECT COUNT(*) FROM mysql.user WHERE User = ?")){
      // If the user doesn't exist, the grant permissions will fail blocking the rest of the user assignments
      // check if the user exists before executing the granting command
      pStmt.setString(1, dbUser);
      resultSet = pStmt.executeQuery();

      if (resultSet.next() && resultSet.getInt(1) != 0) {
        executeUpdate(grantQuery);
      }
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }
  }

  /**
   * Removes a database user for an online featurestore
   *
   * @param dbUser the database-username
   */
  public void removeOnlineFeaturestoreUser(String dbUser) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      try (Connection connection = featureStoreDataSource.getConnection();
           PreparedStatement pStmt = connection.prepareStatement("DROP USER IF EXISTS ?")) {
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
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  public Boolean checkIfDatabaseExists(String dbName) {
    try {
      ResultSet resultSet = null;
      try (Connection connection = featureStoreDataSource.getConnection();
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
    } catch (SQLException se) {
      LOGGER.log(Level.SEVERE, "Error checking if database exists", se);
      return false;
    }
  }

  /**
   * Create a Kafka Offset table in Online Featurestore Database.
   *
   * @param db name of the database
   */
  public void createOnlineFeaturestoreKafkaOffsetTable(String db) throws FeaturestoreException {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    try {
      executeUpdate(
              "CREATE TABLE IF NOT EXISTS `" + db + "`.`kafka_offsets` (\n" +
                      "`topic` varchar(255) COLLATE latin1_general_cs NOT NULL,\n" +
                      "`partition` SMALLINT NOT NULL,\n" +
                      "`offset` BIGINT UNSIGNED NOT NULL,\n" +
                      "PRIMARY KEY (`topic`,`partition`)\n" +
                    ") ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;");
    } catch (SQLException se) {
      throw new FeaturestoreException(
              RESTCodes.FeaturestoreErrorCode.ERROR_CREATING_ONLINE_FEATURESTORE_KAFKA_OFFSET_TABLE,
              Level.SEVERE, "Error running create query", se.getMessage(), se);
    }
  }

  private void executeUpdate(String query) throws SQLException {
    try (Connection connection = featureStoreDataSource.getConnection();
         Statement stmt = connection.createStatement()) {
      stmt.executeUpdate(query);
    }
  }

}
