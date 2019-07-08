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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hive.HiveTableType;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
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
import java.util.regex.Pattern;
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
  private FeaturestoreFacade featurestoreFacade;


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
    String sqlSchema = parseSqlSchemaResult(getSQLSchemaForFeaturegroup(featuregroupDTO,
        featurestore.getProject(), user, featurestore));
    ColumnValueQueryResult column = new ColumnValueQueryResult("schema", sqlSchema);
    List<ColumnValueQueryResult> columns = new ArrayList<>();
    columns.add(column);
    return new RowValueQueryResult(columns);
  }

  /**
   * SHOW CREATE TABLE tblName in Hive returns a table with a single column but multiple rows (cut by String length)
   * this utility method converts the list of rows into a single long string indented with "\n" between rows.
   *
   * @param rows rows result from running SHOW CREATE TABLE
   * @return String representation of SHOW CREATE TABLE in Hive
   */
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
  private String getTblName(String featuregroupName, Integer version) {
    return featuregroupName + "_" + version.toString();
  }

  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table
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
  public List<RowValueQueryResult> getFeaturegroupPreview(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    String tbl = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "SELECT * FROM " + tbl + " LIMIT 20";
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    return executeReadHiveQuery(query, db, featurestore.getProject(), user);
  }

  /**
   * Persists a cached feature group
   *
   * @param featurestore the featurestore of the feature group
   * @param featuregroup the featuregroup
   * @param cachedFeaturegroupDTO the user input data to use when creating the cached feature group
   * @return a DTO representation of the created cached feature group
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public CachedFeaturegroupDTO createCachedFeaturegroup(
      Featurestore featurestore, Featuregroup featuregroup, CachedFeaturegroupDTO cachedFeaturegroupDTO, Users user)
      throws FeaturestoreException, HopsSecurityException, SQLException {
    //Verify User Input
    verifyCachedFeaturegroupUserInput(cachedFeaturegroupDTO);
    //Create Hive Table
    String featureStr = makeCreateTableColumnsStr(cachedFeaturegroupDTO.getFeatures(),
        cachedFeaturegroupDTO.getDescription());
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    String query = "CREATE TABLE " + db + ".`" + tableName + "` " +
        featureStr + " STORED AS " + settings.getFeaturestoreDbDefaultStorageFormat();
    executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
    //Get HiveTblId of the newly created table from the metastore
    Long hiveTblId = cachedFeaturegroupFacade.getHiveTableId(tableName, featurestore.getHiveDbId());
    //Persist cached feature group
    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setFeaturegroup(featuregroup);
    cachedFeaturegroup.setHiveTableId(hiveTblId);
    cachedFeaturegroupFacade.persist(cachedFeaturegroup);
    return convertCachedFeaturegroupToDTO(cachedFeaturegroup);
  }

  /**
   * Converts a CachedFeaturegroup entity into a DTO representation
   *
   * @param cachedFeaturegroup the entity to convert
   * @return the converted DTO representation
   */
  public CachedFeaturegroupDTO convertCachedFeaturegroupToDTO(CachedFeaturegroup cachedFeaturegroup) {
    CachedFeaturegroupDTO cachedFeaturegroupDTO = new CachedFeaturegroupDTO(cachedFeaturegroup);
    List<FeatureDTO> featureDTOs = cachedFeaturegroupFacade.getHiveFeatures(cachedFeaturegroup.getHiveTableId());
    String primaryKeyName = cachedFeaturegroupFacade.getHiveTablePrimaryKey(cachedFeaturegroup.getHiveTableId());
    if(!featureDTOs.isEmpty() && !Strings.isNullOrEmpty(primaryKeyName)){
      featureDTOs.stream().filter(f -> f.getName().equals(primaryKeyName))
          .collect(Collectors.toList()).get(0).setPrimary(true);
    }
    cachedFeaturegroupDTO.setFeatures(featureDTOs);
    String featuregroupName = cachedFeaturegroupFacade.getHiveTableName(cachedFeaturegroup.getHiveTableId());
    //Remove the _version suffix
    int versionLength = cachedFeaturegroupDTO.getVersion().toString().length();
    featuregroupName = featuregroupName.substring(0, featuregroupName.length() - (1 + versionLength));
    cachedFeaturegroupDTO.setName(featuregroupName);
    List<String> hdfsStorePaths = cachedFeaturegroupFacade.getHiveTableHdfsPaths(cachedFeaturegroup.getHiveTableId());
    cachedFeaturegroupDTO.setHdfsStorePaths(hdfsStorePaths);
    cachedFeaturegroupDTO.setDescription(cachedFeaturegroupFacade.getHiveTableComment(
        cachedFeaturegroup.getHiveTableId()));
    Long inodeId = cachedFeaturegroupFacade.getHiveTableInodeId(cachedFeaturegroup.getHiveTableId());
    cachedFeaturegroupDTO.setInodeId(inodeId);
    HiveTableType hiveTableType = cachedFeaturegroupFacade.getHiveTableType(cachedFeaturegroup.getHiveTableId());
    cachedFeaturegroupDTO.setHiveTableType(hiveTableType);
    String hiveInputFormat = cachedFeaturegroupFacade.getHiveInputFormat(cachedFeaturegroup.getHiveTableId());
    cachedFeaturegroupDTO.setInputFormat(hiveInputFormat);
    cachedFeaturegroupDTO.setLocation(hdfsStorePaths.get(0));
    cachedFeaturegroupDTO.setFeaturestoreName(featurestoreFacade.getHiveDbName(
        cachedFeaturegroup.getFeaturegroup().getFeaturestore().getHiveDbId()));
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
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
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
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "DROP TABLE IF EXISTS `" + tableName + "`";
    executeUpdateHiveQuery(query, db, featurestore.getProject(), user);
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
      if (e.getMessage().toLowerCase().contains("permission denied"))
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
            "project: " + project.getName() +
                ", hive database: " + databaseName + " hive query: " + query, e.getMessage(), e);
      else
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HIVE_UPDATE_STATEMENT_ERROR, Level.SEVERE,
            "project: " + project.getName() +
                ", hive database: " + databaseName + " hive query: " + query,
            e.getMessage(), e);
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
  private List<RowValueQueryResult> parseResultset(ResultSet rs) throws SQLException {
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
   * Verify user input specific for creation of on-demand training dataset
   *
   * @param cachedFeaturegroupDTO the user input data for creating the feature group
   */
  public void verifyCachedFeaturegroupUserInput(CachedFeaturegroupDTO cachedFeaturegroupDTO) {

    Pattern namePattern = Pattern.compile(FeaturestoreClientSettingsDTO.FEATURESTORE_REGEX);

    if(cachedFeaturegroupDTO.getName().length() > FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_NAME_MAX_LENGTH ||
        !namePattern.matcher(cachedFeaturegroupDTO.getName()).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
          + ", the name of a cached feature group should be less than "
          + FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_NAME_MAX_LENGTH + " characters and match " +
          "the regular expression: " +  FeaturestoreClientSettingsDTO.FEATURESTORE_REGEX);
    }

    if(!Strings.isNullOrEmpty(cachedFeaturegroupDTO.getDescription()) &&
        cachedFeaturegroupDTO.getDescription().length() >
          FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_DESCRIPTION.getMessage()
          + ", the descritpion of a cached feature group should be less than "
          + FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH + " characters");
    }

    cachedFeaturegroupDTO.getFeatures().stream().forEach(f -> {
      if(Strings.isNullOrEmpty(f.getName()) || !namePattern.matcher(f.getName()).matches() || f.getName().length() >
          FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH){
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME.getMessage()
            + ", the feature name in a cached feature group should be less than "
            + FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH + " characters and match " +
            "the regular expression: " +  FeaturestoreClientSettingsDTO.FEATURESTORE_REGEX);
      }
      if(!Strings.isNullOrEmpty(f.getDescription()) && f.getDescription().length() >
            FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION.getMessage()
            + ", the feature description in a cached feature group should be less than "
            + FeaturestoreClientSettingsDTO.CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH + " characters");
      }
    });
  }

  /**
   * Returns a String with Columns from a JSON featuregroup
   * that can be used for a HiveQL CREATE TABLE statement
   *
   * @param features list of featureDTOs
   * @param featuregroupDoc description of the featuregroup
   * @return feature schema string for creating hive table
   */
  public String makeCreateTableColumnsStr(List<FeatureDTO> features, String featuregroupDoc)
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
    FeatureDTO primaryKey = primaryKeys.get(0);
    if(primaryKey.getPartition()){
      LOGGER.fine("The primary key column: " + primaryKey.getName() +
          " was specified as a partition column, which is not " +
          "allowed. Primary key columns can not be partitioned; Ignoring this partition request.");
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
        schemaStringBuilder.append(feature.getType());
        schemaStringBuilder.append(" COMMENT '");
        schemaStringBuilder.append(feature.getDescription());
        schemaStringBuilder.append("'");
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
        partitionStringBuilder.append(feature.getType());
        partitionStringBuilder.append(" COMMENT '");
        partitionStringBuilder.append(feature.getDescription());
        partitionStringBuilder.append("'");
      }
      if (i == features.size() - 1){
        schemaStringBuilder.append("PRIMARY KEY (`");
        schemaStringBuilder.append(primaryKey.getName());
        schemaStringBuilder.append("`) DISABLE NOVALIDATE) COMMENT '");
        schemaStringBuilder.append(featuregroupDoc);
        schemaStringBuilder.append("' ");
        if(numPartitions > 0){
          partitionStringBuilder.append(")");
          schemaStringBuilder.append(" ");
          schemaStringBuilder.append(partitionStringBuilder.toString());
        }
      }
    }
    return schemaStringBuilder.toString();
  }

}
