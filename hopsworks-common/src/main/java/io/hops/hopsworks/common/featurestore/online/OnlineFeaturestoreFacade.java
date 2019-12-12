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

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A facade for the online feature store databases (separate from the Hopsworks databases).
 * This interface is supposed to be used for any DDL queries on online feature store databases. DML and analytic
 * queries are done through JDBC and client-libraries.
 *
 * Online-Featurestore has its own JDBC Connection Pool defined in persistence unit `featurestorePU`
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class OnlineFeaturestoreFacade {
  @PersistenceContext(unitName = "featurestorePU")
  private EntityManager em;
  
  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @return the size in MB
   */
  public Double getDbSize(String dbName) {
    try {
      return ((BigDecimal) em.createNativeQuery("SELECT " +
        "ROUND(SUM(`tables`.`data_length` + `index_length`) / 1024 / 1024, 1) AS 'size_mb' " +
        "FROM information_schema.`tables` " +
        "WHERE `tables`.`table_schema`=? GROUP BY `tables`.`table_schema`")
        .setParameter(1, dbName)
        .getSingleResult()).doubleValue();
    } catch (NoResultException e) {
      return 0.0;
    }
  }
  
  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @param tableName the name of the table
   * @return the size in MB
   */
  public Double getTblSize(String tableName, String dbName) {
    try {
      return ((BigDecimal) em.createNativeQuery(
        "SELECT round(((`TABLES`.`data_length` + `TABLES`.`index_length`) / 1024 / 1024), 2) `size` " +
          "FROM information_schema.`TABLES` WHERE table_schema=? AND table_name=?;")
        .setParameter(1, dbName)
        .setParameter(2, tableName)
        .getSingleResult()).doubleValue();
    } catch (NoResultException e) {
      return 0.0;
    }
  }
  
  /**
   * Gets the features of a online featuregroup from the MySQL metadata
   *
   * @param tableName the name of the table of the online featuregroup
   * @param db the name of the mysql database
   * @return list of featureDTOs with name,type,comment
   */
  public List<FeatureDTO> getMySQLFeatures(String tableName, String db) {
    List<Object[]> featureObjects = em.createNativeQuery("SELECT `COLUMNS`.`COLUMN_NAME`, `COLUMNS`.`COLUMN_TYPE`, " +
      "`COLUMNS`.`COLUMN_COMMENT` FROM " +
      "INFORMATION_SCHEMA.`COLUMNS` WHERE `COLUMNS`.`TABLE_NAME`=? AND `COLUMNS`.`TABLE_SCHEMA`=?;")
      .setParameter(1, tableName)
      .setParameter(2, db).getResultList();
    ArrayList<FeatureDTO> featureDTOs = new ArrayList<>();
    for (Object[] featureObject : featureObjects) {
      FeatureDTO featureDTO = new FeatureDTO((String) featureObject[0], (String) featureObject[1],
        (String) featureObject[2]);
      featureDTOs.add(featureDTO);
    }
    return featureDTOs;
  }
  
  /**
   * Gets the features of a online featuregroup from the MySQL metadata
   *
   * @param tableName the name of the table of the online featuregroup
   * @param db the name of the mysql database
   * @return list of featureDTOs with name,type,comment
   */
  public String getMySQLSchema(String tableName, String db) {
    List<Object[]> schemaObjects = em.createNativeQuery("SHOW CREATE TABLE `" + db + "`.`" + tableName + "`;")
      .getResultList();
    return (String) schemaObjects.get(0)[1];
  }
  
  /**
   * Gets the type of a MySQL table
   *
   * @param tableName name of the table
   * @param db database where the table resides
   * @return the table type
   */
  public String getMySQLTableType(String tableName, String db) {
    try {
      return (String) em.createNativeQuery("SELECT `TABLES`.`TABLE_TYPE` FROM INFORMATION_SCHEMA.`TABLES` WHERE "
        + "`TABLES`.`table_name`=? AND `TABLES`.`table_schema`=?;")
        .setParameter(1, tableName)
        .setParameter(2, db)
        .getSingleResult();
    } catch (NoResultException e) {
      return "-";
    }
  }
  
  /**
   * Gets the number of rows in a MySQL table
   *
   * @param tableName name of the table
   * @param db database where the table resides
   * @return the table type
   */
  public Integer getMySQLTableRows(String tableName, String db) {
    try {
      return ((BigInteger) em.createNativeQuery("SELECT `TABLES`.`TABLE_ROWS` FROM INFORMATION_SCHEMA.`TABLES` WHERE "
        + "`TABLES`.`table_name`=? AND `TABLES`.`table_schema`=?;")
        .setParameter(1, tableName)
        .setParameter(2, db)
        .getSingleResult()).intValue();
    } catch (NoResultException e) {
      return 0;
    }
  }
  
  /**
   * Create an Online Featurestore Database. Fails if the database already exists.
   *
   * @param db name of the table
   */
  public void createOnlineFeaturestoreDatabase(String db) {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    //Don't add 'IF EXISTS', this call should fail if the database already exists
    em.createNativeQuery("CREATE DATABASE " + db + ";").executeUpdate();
  }
  
  /**
   * Removes an Online Featurestore Database
   *
   * @param db name of the table
   */
  public void removeOnlineFeaturestoreDatabase(String db) {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    em.createNativeQuery("DROP DATABASE IF EXISTS " + db + ";").executeUpdate();
  }
  
  /**
   * Create an Online Featurestore Databasse User
   *
   * @param user the database username
   * @param pw the database user password
   */
  public void createOnlineFeaturestoreUser(String user, String pw) {
    em.createNativeQuery("CREATE USER IF NOT EXISTS ? IDENTIFIED BY ?;")
      .setParameter(1, user)
      .setParameter(2, pw)
      .executeUpdate();
  }
  
  /**
   * Revokes user privileges for a user on a specific online featurestore
   *
   * @param dbName name of the MYSQL database
   * @param dbUser the database username to revoke privileges for
   */
  public void revokeUserPrivileges(String dbName, String dbUser) {
    try{
      // If the grant does not exists, MySQL returns a 1141 error which JPA catches and logs it together with the stack
      // trace, polluting the logs. To avoid this we first query the information_schema to check that the grant exists,
      // if so, we remove it
      int numGrants = (int) em.createNativeQuery(
          "SELECT COUNT(*) FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ?")
          .setParameter(1, dbUser)
          .setParameter(2, dbName)
          .getSingleResult();

      if (numGrants != 0) {
        //Prepared statements with parameters can only be done for
        //WHERE/HAVING Clauses, not names of tables or databases
        em.createNativeQuery("REVOKE ALL PRIVILEGES ON " + dbName + ".* FROM " + dbUser + ";").executeUpdate();
      }
    } catch (Exception e) {
      //This is fine since it might mean that the user does not have the privileges or does not exist
    }
  }
  
  /**
   * Grant database privileges of a "data owner" role in a online featurestore
   *
   * @param dbName name of the online featurestore database
   * @param dbUser the database-username
   */
  public void grantDataOwnerPrivileges(String dbName, String dbUser) {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    em.createNativeQuery("GRANT ALL PRIVILEGES ON " + dbName + ".* TO " + dbUser + ";").executeUpdate();
  }
  
  /**
   * Grant database privileges of a "data scientist" role in a online featurestore
   *
   * @param dbName name of the online featurestore database
   * @param dbUser the database-username
   */
  public void grantDataScientistPrivileges(String dbName, String dbUser) {
    em.createNativeQuery("GRANT SELECT ON " + dbName + ".* TO " + dbUser + ";").executeUpdate();
  }
  
  /**
   * Removes a database user for an online featurestore
   *
   * @param dbUser the database-username
   */
  public void removeOnlineFeaturestoreUser(String dbUser) {
    //Prepared statements with parameters can only be done for
    //WHERE/HAVING Clauses, not names of tables or databases
    em.createNativeQuery("DROP USER IF EXISTS ?")
      .setParameter(1, dbUser)
      .executeUpdate();
  }
  
  /**
   * Get all users for a particular mysql online feature store database
   *
   * @param dbName name of the online featurestore database
   * @return a list of db-usernames for the database
   */
  public List<String> getDatabaseUsers(String dbName) {
    List<String> users = em.createNativeQuery("SELECT `User` FROM `mysql`.`user` WHERE `User` LIKE ?")
      .setParameter(1, dbName + "_%")
      .getResultList();
    return users;
  }
  

  /**
   * Checks if a mysql database exists
   *
   * @param dbName the name of the database
   * @return true or false depending on if the database exists or not
   */
  public Boolean checkIfDatabaseExists(String dbName) {
    try {
      String db = (String) em.createNativeQuery("SELECT `SCHEMA_NAME` FROM `INFORMATION_SCHEMA`.`SCHEMATA` WHERE " +
        "`SCHEMA_NAME`=?")
        .setParameter(1, dbName)
        .getSingleResult();
      if(db != null){
        return true;
      } else {
        return false;
      }
    }
    catch (NoResultException e) {
      return false;
    }
  }
  
}
