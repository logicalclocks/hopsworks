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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.online.OnlineFeaturegroup;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.common.hive.HiveTableType;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the cached_feature_group table and required business logic
 */
@Stateless
public class CachedFeaturegroupController {
  @EJB
  private CachedFeaturegroupFacade cachedFeaturegroupFacade;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private HopsFSProvenanceController fsController;
  @EJB
  private FeaturestoreInputValidation featurestoreInputValidation;

  private static final Logger LOGGER = Logger.getLogger(CachedFeaturegroupController.class.getName());
  private static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";

  @PostConstruct
  public void init() {
    try {
      // Load Hive JDBC Driver
      Class.forName(HIVE_DRIVER);
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Could not load the Hive driver: " + HIVE_DRIVER, e);
    }
  }

  /**
   * Initializes a JDBC connection (thrift RPC) to HS2 using SSL with a given project user and database
   *
   * @param databaseName name of the Hive database to open a connection to
   * @param project      the project of the user making the request
   * @param user         the user making the request
   * @return conn the JDBC connection
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private Connection initConnection(String databaseName, Project project, Users user) throws FeaturestoreException {
    try {
      //Materialize certs
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());

      //Read password
      String password = String.copyValueOf(
          certificateMaterializer.getUserMaterial(user.getUsername(), project.getName()).getPassword());

      // Create connection url
      String hiveEndpoint = settings.getHiveServerHostName(false);
      String jdbcString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
          "auth=noSasl;ssl=true;twoWay=true;" +
          "sslTrustStore=" + certificateMaterializer.getUserTransientTruststorePath(project, user) + ";" +
          "trustStorePassword=" + password + ";" +
          "sslKeyStore=" + certificateMaterializer.getUserTransientKeystorePath(project, user) + ";" +
          "keyStorePassword=" + password;
  
      return DriverManager.getConnection(jdbcString);
    } catch (FileNotFoundException | CryptoPasswordNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Could not find user certificates for authenticating with Hive: " +
          e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CERTIFICATES_NOT_FOUND, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    } catch (SQLException | IOException e) {
      LOGGER.log(Level.SEVERE, "Error initiating Hive connection: " +
          e);
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_HIVE_CONNECTION, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    }
  }

  /**
   * Executes "SHOW CREATE TABLE" on the hive table of the featuregroup formats it as a string and returns it
   *
   * @param featuregroupDTO the featuregroup to get the schema for
   * @param user            the user making the request
   * @param featurestore    the featurestore where the featuregroup resides
   * @return                JSON/XML DTO with the schema
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public RowValueQueryResult getDDLSchema(FeaturegroupDTO featuregroupDTO, Users user, Featurestore featurestore)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    if(featuregroupDTO.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.CANNOT_FETCH_HIVE_SCHEMA_FOR_ON_DEMAND_FEATUREGROUPS,
          Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
    }
    String offlineSqlSchema = parseSqlSchemaResult(getSQLSchemaForFeaturegroup(featuregroupDTO,
        featurestore.getProject(), user, featurestore));
    ColumnValueQueryResult offlineSchemaColumn = new ColumnValueQueryResult("schema", offlineSqlSchema);
    List<ColumnValueQueryResult> columns = new ArrayList<>();
    columns.add(offlineSchemaColumn);
    if(settings.isOnlineFeaturestore() && ((CachedFeaturegroupDTO)featuregroupDTO).getOnlineFeaturegroupDTO() != null) {
      String onlineSqlSchema =
        onlineFeaturegroupController.getOnlineFeaturegroupSchema(
          ((CachedFeaturegroupDTO)featuregroupDTO).getOnlineFeaturegroupDTO());
      ColumnValueQueryResult onlineSchemaColumn = new ColumnValueQueryResult("onlineSchema", onlineSqlSchema);
      columns.add(onlineSchemaColumn);
    }
    return new RowValueQueryResult(columns);
  }

  /**
   * SHOW CREATE TABLE tblName in Hive returns a table with a single column but multiple rows (cut by String length)
   * this utility method converts the list of rows into a single long string indented with "\n" between rows.
   *
   * @param rows rows result from running SHOW CREATE TABLE
   * @return String representation of SHOW CREATE TABLE in Hive
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private String parseSqlSchemaResult(List<RowValueQueryResult> rows) {
    return StringUtils.join(rows.stream().map
            (row -> StringUtils.join(row.getColumns().stream().map
                    (column -> column.getValue()).collect(Collectors.toList()),
                "")).collect(Collectors.toList()),
        "\n");
  }

  /**
   * Gets the featuregroup Hive table name
   *
   * @param featuregroupName name of the featuregroup
   * @param version          version of the featuregroup
   * @return                 the hive table name of the featuregroup (featuregroup_version)
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private String getTblName(String featuregroupName, Integer version) {
    return featuregroupName + "_" + version.toString();
  }

  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table (offline feature data)
   * and the MySQL table (online feature data)
   *
   * @param featuregroupDTO DTO of the featuregroup to preview
   * @param featurestore    the feature store where the feature group resides
   * @param user            the user making the request
   * @return A DTO with the first 20 feature rows of the online and offline tables.
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupPreview getFeaturegroupPreview(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    FeaturegroupPreview featuregroupPreview = new FeaturegroupPreview();
    List<RowValueQueryResult> offlinePreview = getOfflineFeaturegroupPreview(featuregroupDTO, featurestore, user);
    featuregroupPreview.setOfflineFeaturegroupPreview(offlinePreview);
    if(settings.isOnlineFeaturestore() && ((CachedFeaturegroupDTO) featuregroupDTO).getOnlineFeaturegroupEnabled()){
      List<RowValueQueryResult> onlinePreview =
        onlineFeaturegroupController.getOnlineFeaturegroupPreview(
          ((CachedFeaturegroupDTO) featuregroupDTO).getOnlineFeaturegroupDTO(), user, featurestore);
      featuregroupPreview.setOnlineFeaturegroupPreview(onlinePreview);
    }
    return featuregroupPreview;
  }
  
  /**
   * Previews the offline data of a given featuregroup by doing a SELECT LIMIT query on the Hive Table
   *
   * @param featuregroupDTO DTO of the featuregroup to preview
   * @param featurestore    the feature store where the feature group resides
   * @param user            the user making the request
   * @return list of feature-rows from the Hive table where the featuregroup is stored
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getOfflineFeaturegroupPreview(FeaturegroupDTO featuregroupDTO,
    Featurestore featurestore, Users user) throws FeaturestoreException, HopsSecurityException, SQLException {
    String tbl = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "SELECT * FROM " + tbl + " LIMIT 20";
    String db = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    try {
      return executeReadHiveQuery(query, db, featurestore.getProject(), user);
    } catch(Exception e) {
      return executeReadHiveQuery(query, db, featurestore.getProject(), user);
    }
  }

  /**
   * Persists a cached feature group
   *
   * @param featurestore the featurestore of the feature group
   * @param cachedFeaturegroupDTO the user input data to use when creating the cached feature group
   * @param user the user making the request
   * @return the created entity
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public CachedFeaturegroup createCachedFeaturegroup(
      Featurestore featurestore, CachedFeaturegroupDTO cachedFeaturegroupDTO, Users user)
    throws FeaturestoreException, HopsSecurityException, SQLException, ProvenanceException {

    //Prepare DDL statement
    String hiveFeatureStr = makeCreateTableColumnsStr(cachedFeaturegroupDTO.getFeatures(),
      cachedFeaturegroupDTO.getDescription(), false);
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    
    //Create Hive Table for Offline Cached Feature Group
    createHiveFeaturegroup(cachedFeaturegroupDTO, featurestore, user, hiveFeatureStr, tableName);
    fsController.featuregroupAttachXAttrs(user, featurestore.getProject(), cachedFeaturegroupDTO);
  
    //Create MySQL Table for Online Cached Feature Group
    OnlineFeaturegroup onlineFeaturegroup = null;
    if(settings.isOnlineFeaturestore() && cachedFeaturegroupDTO.getOnlineFeaturegroupEnabled()){
      String mySQLFeatureStr = makeCreateTableColumnsStr(cachedFeaturegroupDTO.getFeatures(),
        cachedFeaturegroupDTO.getDescription(), true);
      onlineFeaturegroup = onlineFeaturegroupController.createMySQLTable(featurestore, user, mySQLFeatureStr,
        tableName);
    }
    
    //Get HiveTblId of the newly created table from the metastore
    Long hiveTblId = cachedFeaturegroupFacade.getHiveTableId(tableName, featurestore.getHiveDbId());
    
    //Persist cached feature group
    return persistCachedFeaturegroupMetadata(hiveTblId, onlineFeaturegroup);
  }
  
  /**
   * Creates Hive Database for an offline feature group
   *
   * @param cachedFeaturegroupDTO the user input data to use when creating the offline cached feature group
   * @param featurestore featurestore the featurestore that the cached featuregroup belongs to
   * @param user user the user making the request
   * @param featureStr DDL string
   * @param tableName name of the table to create
   * @throws FeaturestoreException
   * @throws SQLException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void createHiveFeaturegroup(CachedFeaturegroupDTO cachedFeaturegroupDTO, Featurestore featurestore,
    Users user, String featureStr, String tableName)
    throws FeaturestoreException, SQLException, HopsSecurityException {
    //Create Hive Table
    String db = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    String query = "CREATE TABLE " + db + ".`" + tableName + "` " +
      featureStr + "STORED AS " + settings.getFeaturestoreDbDefaultStorageFormat();
    try{
      executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
    } catch(Exception e) { //Retry once
      executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
    }
  }

  /**
   * Converts a CachedFeaturegroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public CachedFeaturegroupDTO convertCachedFeaturegroupToDTO(Featuregroup featuregroup) {
    CachedFeaturegroupDTO cachedFeaturegroupDTO = new CachedFeaturegroupDTO(featuregroup);
    List<FeatureDTO> featureDTOs =
      cachedFeaturegroupFacade.getHiveFeatures(featuregroup.getCachedFeaturegroup().getHiveTableId());
    List<String> primaryKeys = cachedFeaturegroupFacade.getHiveTablePrimaryKey(
      featuregroup.getCachedFeaturegroup().getHiveTableId());
    if(!featureDTOs.isEmpty() && !primaryKeys.isEmpty()) {
      featureDTOs.stream().filter(f -> primaryKeys.contains(f.getName())).forEach(f -> f.setPrimary(true));
    }
    if (settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup() != null) {
      List<FeatureDTO> onlineFeatureDTOs =
        onlineFeaturegroupController.getOnlineFeaturegroupFeatures(
          featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup());
      for (FeatureDTO featureDTO: featureDTOs) {
        for (FeatureDTO onlineFeatureDTO: onlineFeatureDTOs) {
          if(featureDTO.getName().equalsIgnoreCase(onlineFeatureDTO.getName())){
            featureDTO.setOnlineType(onlineFeatureDTO.getType());
          }
        }
      }
    }
    cachedFeaturegroupDTO.setFeatures(featureDTOs);
    cachedFeaturegroupDTO.setName(featuregroup.getName());
    List<String> hdfsStorePaths = cachedFeaturegroupFacade.getHiveTableHdfsPaths(
      featuregroup.getCachedFeaturegroup().getHiveTableId());
    cachedFeaturegroupDTO.setHdfsStorePaths(hdfsStorePaths);
    cachedFeaturegroupDTO.setDescription(cachedFeaturegroupFacade.getHiveTableComment(
        featuregroup.getCachedFeaturegroup().getHiveTableId()));
    Long inodeId = cachedFeaturegroupFacade.getHiveTableInodeId(featuregroup.getCachedFeaturegroup().getHiveTableId());
    cachedFeaturegroupDTO.setInodeId(inodeId);
    HiveTableType hiveTableType = cachedFeaturegroupFacade.getHiveTableType(
      featuregroup.getCachedFeaturegroup().getHiveTableId());
    cachedFeaturegroupDTO.setHiveTableType(hiveTableType);
    String hiveInputFormat = cachedFeaturegroupFacade.getHiveInputFormat(
      featuregroup.getCachedFeaturegroup().getHiveTableId());
    cachedFeaturegroupDTO.setInputFormat(hiveInputFormat);
    cachedFeaturegroupDTO.setLocation(hdfsStorePaths.get(0));
    if (settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup() != null){
      cachedFeaturegroupDTO.setOnlineFeaturegroupEnabled(true);
      cachedFeaturegroupDTO.setOnlineFeaturegroupDTO(onlineFeaturegroupController.convertOnlineFeaturegroupToDTO(
        featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup()));
    }
    return cachedFeaturegroupDTO;
  }

  /**
   * Gets the SQL schema that was used to create the Hive table for a featuregroup
   *
   * @param featuregroupDTO DTO of the featuregroup
   * @param project         the project of the user making the request
   * @param user            the user making the request
   * @param featurestore    the featurestore where the featuregroup resides
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<RowValueQueryResult> getSQLSchemaForFeaturegroup(
      FeaturegroupDTO featuregroupDTO,
      Project project, Users user, Featurestore featurestore)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    String tbl = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "SHOW CREATE TABLE " + tbl;
    String db = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    return executeReadHiveQuery(query, db, project, user);
  }

  /**
   * Runs a DROP TABLE statement on a featuregroup of a featurestore hive DB
   *
   * @param featuregroupDTO  information about the featuregroup to delete
   * @param user             the user making the request
   * @param featurestore     the featurestore where the featuregroup resides
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void dropHiveFeaturegroup(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Users user) throws SQLException,
      FeaturestoreException, HopsSecurityException {
    String db = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "DROP TABLE IF EXISTS `" + tableName + "`";
    try {
      executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
    } catch (Exception e) { //Retry once
      executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
    }
  }
  
  /**
   * Drops a online feature group in MySQL database
   *
   * @param cachedFeaturegroup a cached feature group
   * @param featurestore    the featurestore that the featuregroup belongs to
   * @param user            the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void dropMySQLFeaturegroup(
    CachedFeaturegroup cachedFeaturegroup, Featurestore featurestore, Users user) throws SQLException,
    FeaturestoreException {
    if(settings.isOnlineFeaturestore() && cachedFeaturegroup.getOnlineFeaturegroup() != null){
      onlineFeaturegroupController.dropMySQLTable(cachedFeaturegroup.getOnlineFeaturegroup(), featurestore, user);
    }
  }
  

  /**
   * Opens a JDBC connection to HS2 using the given database and project-user and then executes an update
   * SQL query
   *
   * @param query        the update query
   * @param databaseName the name of the Hive database
   * @param project      the project of the user making the request
   * @param user         the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void executeUpdateHiveQuery(String query, String databaseName, Project project, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    //Re-create the connection every time since the connection is database and user-specific
    Statement stmt = null;
    Connection conn = null;
    try {
      conn = initConnection(databaseName, project, user);
      // Create database
      stmt = conn.createStatement();
      stmt.executeUpdate(query);
    } catch (SQLException e) {
      //Hive throws a generic HiveSQLException not a specific AuthorizationException
      if (e.getMessage().toLowerCase().contains("permission denied")){
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
            "project: " + project.getName() +
                ", hive database: " + databaseName + " hive query: " + query, e.getMessage(), e);
      }
      else{
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HIVE_UPDATE_STATEMENT_ERROR, Level.SEVERE,
            "project: " + project.getName() +
                ", hive database: " + databaseName + " hive query: " + query,
            e.getMessage(), e);
      }
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      closeConnection(conn, user, project);
    }
  }

  /**
   * Parses a ResultSet from a Hive query into a list of RowValueQueryResultDTOs
   *
   * @param rs resultset to parse
   * @return list of parsed rows
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> parseResultset(ResultSet rs) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    int columnsNumber = rsmd.getColumnCount();
    List<RowValueQueryResult> rows = new ArrayList<>();
    while (rs.next()) {
      List<ColumnValueQueryResult> columnValues = new ArrayList<>();
      for (int i = 1; i <= columnsNumber; i++) {
        String columnName = rsmd.getColumnName(i);
        Object columnValue = rs.getObject(i);
        String columnStrValue = (columnValue == null ? null : columnValue.toString());
        ColumnValueQueryResult featuredataDTO = new ColumnValueQueryResult(columnName, columnStrValue);
        columnValues.add(featuredataDTO);
      }
      rows.add(new RowValueQueryResult(columnValues));
    }
    return rows;
  }

  /**
   * Opens a JDBC connection to HS2 using the given database and project-user and then executes a regular
   * SQL query
   *
   * @param query        the read query
   * @param databaseName the name of the Hive database
   * @param project      the project that owns the Hive database
   * @param user         the user making the request
   * @return parsed resultset
   * @throws SQLException
   * @throws HopsSecurityException
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<RowValueQueryResult> executeReadHiveQuery(
      String query, String databaseName, Project project, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    Connection conn = null;
    Statement stmt = null;
    List<RowValueQueryResult> resultList = null;
    try {
      //Re-create the connection every time since the connection is database and user-specific
      conn = initConnection(databaseName, project, user);
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      resultList = parseResultset(rs);
    } catch (SQLException e) {
      //Hive throws a generic HiveSQLException not a specific AuthorizationException
      if (e.getMessage().toLowerCase().contains("permission denied"))
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
            "project: " + project.getName() +
                ", hive database: " + databaseName + " hive query: " + query, e.getMessage(), e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HIVE_READ_QUERY_ERROR, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName + " hive query: " + query,
          e.getMessage(), e);
    } finally {
      if (stmt != null) {
        stmt.close();
      }
      closeConnection(conn, user, project);
    }
    return resultList;
  }

  /**
   * Checks if the JDBC connection to HS2 is open, and if so closes it.
   *
   * @param conn the JDBC connection
   * @param user the user using the connection
   * @param project the project where the connection is used
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void closeConnection(Connection conn, Users user, Project project) {
    try {
      if (conn != null) {
        conn.close();
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Error closing Hive JDBC connection: " +  e);
    } finally {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    }
  }

  /**
   * Returns a String with Columns from a JSON featuregroup
   * that can be used for a HiveQL CREATE TABLE statement or MySQL
   *
   * @param features list of featureDTOs
   * @param featuregroupDoc description of the featuregroup
   * @param mysqlTable whether the DDL is to be used for Hive or MYSQL
   * @return feature schema string for creating hive or MYSQL table
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String makeCreateTableColumnsStr(List<FeatureDTO> features, String featuregroupDoc, Boolean mysqlTable)
      throws FeaturestoreException {
    StringBuilder schemaStringBuilder = new StringBuilder();
    StringBuilder partitionStringBuilder = new StringBuilder();
    if(features.isEmpty()) {
      schemaStringBuilder.append("(`temp` int COMMENT 'placeholder') " +
          "COMMENT '");
      schemaStringBuilder.append(featuregroupDoc);
      schemaStringBuilder.append("' ");
      return schemaStringBuilder.toString();
    }
    List<FeatureDTO> primaryKeys = features.stream().filter(f -> f.getPrimary()).collect(Collectors.toList());
    if(primaryKeys.isEmpty()){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NO_PRIMARY_KEY_SPECIFIED, Level.SEVERE,
          "Out of the " + features.size() + " features provided, none is marked as primary");
    }
    for (FeatureDTO primaryKey :primaryKeys) {
      if(primaryKey.getPartition()){
        LOGGER.fine("The primary key column: " + primaryKey.getName() +
          " was specified as a partition column, which is not " +
          "allowed. Primary key columns can not be partitioned; Ignoring this partition request.");
      }
    }
    schemaStringBuilder.append("(");
    int numPartitions = features.stream().filter(f -> f.getPartition()).collect(Collectors.toList()).size();
    partitionStringBuilder.append("PARTITIONED BY (");
    Boolean firstPartition = true;
    for (int i = 0; i < features.size(); i++) {
      FeatureDTO feature = features.get(i);
      if(!feature.getPartition() || feature.getPrimary()){
        schemaStringBuilder.append("`");
        schemaStringBuilder.append(feature.getName());
        schemaStringBuilder.append("` ");
        if(!mysqlTable){
          schemaStringBuilder.append(feature.getType());
        } else {
          schemaStringBuilder.append(feature.getOnlineType());
        }
        if (!Strings.isNullOrEmpty(feature.getDescription())) {
          schemaStringBuilder.append(" COMMENT '");
          schemaStringBuilder.append(feature.getDescription());
          schemaStringBuilder.append("'");
        }
        schemaStringBuilder.append(", ");
      } else {
        if(!firstPartition){
          partitionStringBuilder.append(",");
        } else {
          firstPartition = false;
        }
        partitionStringBuilder.append("`");
        partitionStringBuilder.append(feature.getName());
        partitionStringBuilder.append("` ");
        if(!mysqlTable){
          partitionStringBuilder.append(feature.getType());
        } else {
          partitionStringBuilder.append(feature.getOnlineType());
        }
        if (!Strings.isNullOrEmpty(feature.getDescription())) {
          partitionStringBuilder.append(" COMMENT '");
          partitionStringBuilder.append(feature.getDescription());
          partitionStringBuilder.append("'");
        }
      }
      if (i == features.size() - 1){
        Boolean firstPrimary = true;
        schemaStringBuilder.append("PRIMARY KEY (");
        for (int j = 0; j < primaryKeys.size(); j++) {
          if(!firstPrimary){
            schemaStringBuilder.append(",");
          } else {
            firstPrimary = false;
          }
          schemaStringBuilder.append("`");
          schemaStringBuilder.append(primaryKeys.get(j).getName());
          schemaStringBuilder.append("` ");
        }
        if(!mysqlTable){
          schemaStringBuilder.append(") DISABLE NOVALIDATE) COMMENT '");
        } else {
          schemaStringBuilder.append(")) COMMENT '");
        }
        schemaStringBuilder.append(featuregroupDoc);
        schemaStringBuilder.append("' ");
        if(numPartitions > 0 && !mysqlTable){
          partitionStringBuilder.append(")");
          schemaStringBuilder.append(" ");
          schemaStringBuilder.append(partitionStringBuilder.toString());
        }
      }
    }
    return schemaStringBuilder.toString();
  }
  
  /**
   * Synchronizes an already created Hive table with the Feature Store metadata
   *
   * @param featurestore the featurestore of the feature group
   * @param cachedFeaturegroupDTO the feature group DTO
   * @return a DTO of the created feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public CachedFeaturegroup syncHiveTableWithFeaturestore(Featurestore featurestore,
    CachedFeaturegroupDTO cachedFeaturegroupDTO) throws FeaturestoreException {
  
    //Get Hive Table Metadata
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    Long hiveTblId = cachedFeaturegroupFacade.getHiveTableId(tableName, featurestore.getHiveDbId());
    if(hiveTblId == null){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.SYNC_TABLE_NOT_FOUND, Level.FINE,
        ", tried to sync hive table with name: " + tableName + " with the feature store, but the table was not found " +
          "in the Hive metastore");
    }
  
    //Persist cached feature group
    return persistCachedFeaturegroupMetadata(hiveTblId, null);
  }
  
  
  /**
   * Persists metadata of a new cached feature group in the cached_feature_group table
   *
   * @param hiveTblId the id of the Hive table in the Hive metastore
   * @return Entity of the created cached feature group
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private CachedFeaturegroup persistCachedFeaturegroupMetadata(Long hiveTblId, OnlineFeaturegroup onlineFeaturegroup) {
    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setHiveTableId(hiveTblId);
    cachedFeaturegroup.setOnlineFeaturegroup(onlineFeaturegroup);
    cachedFeaturegroupFacade.persist(cachedFeaturegroup);
    return cachedFeaturegroup;
  }
  
  /**
   * Update a cached featuregroup that currently does not support online feature serving, to support it.
   *
   * @param featurestore the featurestore where the featuregroup resides
   * @param cachedFeaturegroupDTO metadata about the online featuregroup to create
   * @param featuregroup the featuregroup entity to update
   * @param user the user making the request
   * @return a DTO of the updated featuregroup
   * @throws FeaturestoreException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO enableFeaturegroupOnline(
    Featurestore featurestore, CachedFeaturegroupDTO cachedFeaturegroupDTO,
    Featuregroup featuregroup, Users user)
    throws FeaturestoreException, SQLException {
    if(!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this Hopsworks cluster.");
    }
    if (!onlineFeaturestoreController.checkIfDatabaseExists(
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject()))) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this project. To enable online feature store, talk to an " +
        "administrator.");
    }
    //Create MySQL Table for Online Feature Group
    OnlineFeaturegroup onlineFeaturegroup = null;
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    if(settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup() == null) {
      String mySQLFeatureStr = makeCreateTableColumnsStr(cachedFeaturegroupDTO.getFeatures(),
        cachedFeaturegroupDTO.getDescription(), true);
      onlineFeaturegroup = onlineFeaturegroupController.createMySQLTable(featurestore, user, mySQLFeatureStr,
        tableName);
    }
    CachedFeaturegroup cachedFeaturegroup = featuregroup.getCachedFeaturegroup();
    //Set foreign key of the cached feature group to the new online feature group
    cachedFeaturegroup.setOnlineFeaturegroup(onlineFeaturegroup);
    cachedFeaturegroupFacade.updateMetadata(cachedFeaturegroup);
    return convertCachedFeaturegroupToDTO(featuregroup);
  }
  
  /**
   * Update a cached featuregroup that currently supports online feature serving, and disable it (drop MySQL db)
   *
   * @param featurestore the featurestore where the featuregroup resides
   * @param featuregroup the featuregroup entity to update
   * @param user the user making the request
   * @return a DTO of the updated featuregroup
   * @throws FeaturestoreException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO disableFeaturegroupOnline(
    Featurestore featurestore, Featuregroup featuregroup, Users user)
    throws FeaturestoreException, SQLException {
    if(!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this Hopsworks cluster.");
    }
    if (!onlineFeaturestoreController.checkIfDatabaseExists(
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject()))) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this project. To enable online feature store, talk to an " +
        "administrator.");
    }
    CachedFeaturegroup cachedFeaturegroup = featuregroup.getCachedFeaturegroup();
    if(settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().getOnlineFeaturegroup() != null) {
      //Drop MySQL Table for Online Feature Group
      dropMySQLFeaturegroup(cachedFeaturegroup, featurestore, user);
    }
    return convertCachedFeaturegroupToDTO(featuregroup);
  }
  
}
