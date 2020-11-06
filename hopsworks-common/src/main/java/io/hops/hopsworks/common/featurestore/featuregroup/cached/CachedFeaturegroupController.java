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
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveColumns;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveKeyConstraints;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HivePartitionKeys;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTableParams;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTbls;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hive.metastore.api.SQLDefaultConstraint;
import org.javatuples.Pair;

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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the cached_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
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
  private OfflineFeatureGroupController offlineFeatureGroupController;
  @EJB
  private HiveController hiveController;
  @EJB
  private ConstructorController constructorController;

  private static final Logger LOGGER = Logger.getLogger(CachedFeaturegroupController.class.getName());
  private static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
  private static final List<String> HUDI_SPEC_FEATURE_NAMES = Arrays.asList("_hoodie_record_key",
      "_hoodie_partition_path", "_hoodie_commit_time", "_hoodie_file_name", "_hoodie_commit_seqno");

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
      // Create connection url
      String hiveEndpoint = hiveController.getHiveServerInternalEndpoint();
      //Materialize certs
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());

      //Read password
      String password = String.copyValueOf(
          certificateMaterializer.getUserMaterial(user.getUsername(), project.getName()).getPassword());

      String jdbcString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
          "auth=noSasl;ssl=true;twoWay=true;" +
          "sslTrustStore=" + certificateMaterializer.getUserTransientTruststorePath(project, user) + ";" +
          "trustStorePassword=" + password + ";" +
          "sslKeyStore=" + certificateMaterializer.getUserTransientKeystorePath(project, user) + ";" +
          "keyStorePassword=" + password;
  
      return DriverManager.getConnection(jdbcString);
    } catch (FileNotFoundException | CryptoPasswordNotFoundException | ServiceDiscoveryException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CERTIFICATES_NOT_FOUND, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    } catch (SQLException | IOException e) {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_HIVE_CONNECTION, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    }
  }

  /**
   * Executes "SHOW CREATE TABLE" on the hive table of the featuregroup formats it as a string and returns it
   *
   * @param featuregroup    the featuregroup to get the schema for
   * @param project         project from which the user is making the request
   * @param user            the user making the request
   * @return                JSON/XML DTO with the schema
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  public String getDDLSchema(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, HopsSecurityException {
    try {
      return parseSqlSchemaResult(getSQLSchemaForFeaturegroup(featuregroup, project, user));
    } catch (SQLException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_FETCH_FEATUREGROUP_SHOW_CREATE_SCHEMA,
          Level.SEVERE, "Internal error fetching the schema of the feature group", e.getMessage(), e);
    }
  }

  /**
   * SHOW CREATE TABLE tblName in Hive returns a table with a single column but multiple rows (cut by String length)
   * this utility method converts the list of rows into a single long string indented with "\n" between rows.
   *
   * @param preview rows result from running SHOW CREATE TABLE
   * @return String representation of SHOW CREATE TABLE in Hive
   */
  private String parseSqlSchemaResult(FeaturegroupPreview preview){
    return StringUtils.join(preview.getPreview().stream()
        .map(row -> row.getValues().get(0).getValue1())
        .collect(Collectors.toList()), "\n");
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
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table (offline feature data)
   * and the MySQL table (online feature data)
   *
   * @param featuregroup    of the featuregroup to preview
   * @param project         the project the user is operating from, in case of shared feature store
   * @param user            the user making the request
   * @param partition       the selected partition if any as represented in the PARTITIONS_METASTORE
   * @param online          whether to show preview from the online feature store
   * @param limit           the number of rows to visualize
   * @return A DTO with the first 20 feature rows of the online and offline tables.
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  public FeaturegroupPreview getFeaturegroupPreview(Featuregroup featuregroup, Project project,
                                                    Users user, String partition, boolean online, int limit)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    if (online && featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
      return onlineFeaturegroupController.getFeaturegroupPreview(featuregroup, project, user, limit);
    } else if (online) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_ONLINE, Level.FINE);
    } else {
      return getOfflineFeaturegroupPreview(featuregroup, project, user, partition, limit);
    }
  }
  
  /**
   * Previews the offline data of a given featuregroup by doing a SELECT LIMIT query on the Hive Table
   *
   * @param featuregroup    the featuregroup to fetch
   * @param project         the project the user is operating from, in case of shared feature store
   * @param user            the user making the request
   * @param limit           number of sample to fetch
   * @return list of feature-rows from the Hive table where the featuregroup is stored
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  public FeaturegroupPreview getOfflineFeaturegroupPreview(Featuregroup featuregroup, Project project,
                                                           Users user, String partition, int limit)
      throws FeaturestoreException, HopsSecurityException, SQLException {
    String tbl = getTblName(featuregroup.getName(), featuregroup.getVersion());
    List<FeatureGroupFeatureDTO> features = getFeaturesDTO(featuregroup.getCachedFeaturegroup().getHiveTbls(),
      featuregroup.getFeaturestore(), project, user);

    // This is not great, but at the same time the query runs as the user.
    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (FeatureGroupFeatureDTO feature : features) {
      if (feature.getDefaultValue() == null) {
        selectList.add(new SqlIdentifier(Arrays.asList("`" + tbl + "`", "`" + feature.getName() + "`"),
          SqlParserPos.ZERO));
      } else {
        selectList.add(constructorController.selectWithDefaultAs(new Feature(feature, tbl)));
      }
    }

    SqlNode whereClause = getWhereCondition(partition, features);

    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList,
      new SqlIdentifier("`" + tbl + "`", SqlParserPos.ZERO),
      whereClause, null, null, null, null, null,
      SqlLiteral.createExactNumeric(String.valueOf(limit), SqlParserPos.ZERO));
    String db = featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    try {
      return executeReadHiveQuery(
        select.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql(), db, project, user);
    } catch(Exception e) {
      return executeReadHiveQuery(
        select.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql(), db, project, user);
    }
  }

  public SqlNode getWhereCondition(String partition, List<FeatureGroupFeatureDTO> features)
      throws FeaturestoreException {
    if (Strings.isNullOrEmpty(partition)) {
      // user didn't ask for a specific partition
      return null;
    }

    // partition names are separated by /, column=VALUE/column=VALUE
    SqlNodeList whereClauses = new SqlNodeList(SqlParserPos.ZERO);
    String[] splits = partition.split("/");
    for (String split : splits) {
      int posEqual = split.indexOf("=");
      String column = split.substring(0, posEqual);
      FeatureGroupFeatureDTO partitionFeature = features.stream()
        .filter(FeatureGroupFeatureDTO::getPartition)
        .filter(feature -> feature.getName().equals(column))
        .findFirst().orElseThrow(() ->
          new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME, Level.FINE,
          "The selected partition column: " + column + " was not found among the partition columns of the feature " +
            "group."));
      SqlNode value;
      if (partitionFeature.getType().equalsIgnoreCase("string")) {
        value = SqlLiteral.createCharString(split.substring(posEqual + 1), SqlParserPos.ZERO);
      } else {
        value = new SqlIdentifier(split.substring(posEqual + 1), SqlParserPos.ZERO);
      }
      whereClauses.add(SqlStdOperatorTable.EQUALS.createCall(
        SqlParserPos.ZERO,
        new SqlIdentifier("`" + column + "`", SqlParserPos.ZERO),
        value));
    }
    if (whereClauses.size() == 1) {
      return whereClauses;
    }
    return SqlStdOperatorTable.AND.createCall(whereClauses);
  }

  /**
   * Persists a cached feature group
   *
   * @param featurestore the featurestore of the feature group
   * @param cachedFeaturegroupDTO the user input data to use when creating the cached feature group
   * @param user the user making the request
   * @return the created entity
   */
  public CachedFeaturegroup createCachedFeaturegroup(
      Featurestore featurestore, CachedFeaturegroupDTO cachedFeaturegroupDTO, Project project, Users user)
      throws FeaturestoreException, ServiceException, IOException, SQLException {

    verifyPrimaryKeyNotPartitionKey(cachedFeaturegroupDTO.getFeatures());

    // add hudi specific metadata features, as user doesn't provide it
    if (cachedFeaturegroupDTO.getTimeTravelFormat() == TimeTravelFormat.HUDI){
      cachedFeaturegroupDTO.setFeatures(addHudiSpecFeatures(cachedFeaturegroupDTO.getFeatures()));
    }

    //Prepare DDL statement
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    offlineFeatureGroupController.createHiveTable(featurestore, tableName, cachedFeaturegroupDTO.getDescription(),
        cachedFeaturegroupDTO.getFeatures(), project, user);

    //Create MySQL Table for Online Cached Feature Group
    boolean onlineEnabled = false;
    if(settings.isOnlineFeaturestore() && cachedFeaturegroupDTO.getOnlineEnabled()){
      onlineFeaturegroupController
          .createMySQLTable(featurestore, tableName, cachedFeaturegroupDTO.getFeatures(), project, user);
      onlineEnabled = true;
    }
    
    //Get HiveTblId of the newly created table from the metastore
    HiveTbls hiveTbls = cachedFeaturegroupFacade.getHiveTableByNameAndDB(tableName, featurestore.getHiveDbId())
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP,
            Level.WARNING, "", "Table created correctly but not in the metastore"));

    //Persist cached feature group
    return persistCachedFeaturegroupMetadata(hiveTbls, onlineEnabled, cachedFeaturegroupDTO.getTimeTravelFormat());
  }
  
  /**
   * Converts a CachedFeaturegroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  public CachedFeaturegroupDTO convertCachedFeaturegroupToDTO(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException {
    CachedFeaturegroupDTO cachedFeaturegroupDTO = new CachedFeaturegroupDTO(featuregroup);
    HiveTbls hiveTable = featuregroup.getCachedFeaturegroup().getHiveTbls();
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS =
      getFeaturesDTO(hiveTable, featuregroup.getFeaturestore(), project, user);

    if (settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
      cachedFeaturegroupDTO.setOnlineEnabled(true);
      List<FeatureGroupFeatureDTO> onlineFeatureGroupFeatureDTOS =
          onlineFeaturegroupController.getFeaturegroupFeatures(featuregroup);
      for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOS) {
        for (FeatureGroupFeatureDTO onlineFeatureGroupFeatureDTO : onlineFeatureGroupFeatureDTOS) {
          if(featureGroupFeatureDTO.getName().equalsIgnoreCase(onlineFeatureGroupFeatureDTO.getName())){
            featureGroupFeatureDTO.setOnlineType(onlineFeatureGroupFeatureDTO.getType());
          }
        }
      }
    }
    cachedFeaturegroupDTO.setFeatures(featureGroupFeatureDTOS);
    cachedFeaturegroupDTO.setName(featuregroup.getName());
    cachedFeaturegroupDTO.setTimeTravelFormat(featuregroup.getCachedFeaturegroup().getTimeTravelFormat());
    cachedFeaturegroupDTO.setHudiEnabled(featuregroup.getCachedFeaturegroup().getHiveTbls()
            .getSdId().getInputFormat().equals(OfflineFeatureGroupController.Formats.HUDI.getInputFormat()));

    cachedFeaturegroupDTO.setDescription(hiveTable.getHiveTableParamsCollection().stream()
        .filter(p -> p.getHiveTableParamsPK().getParamKey().equalsIgnoreCase("COMMENT"))
        .map(HiveTableParams::getParamValue)
        .findFirst()
        .orElse("")
    );

    cachedFeaturegroupDTO.setLocation(hiveTable.getSdId().getLocation());
    return cachedFeaturegroupDTO;
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTO(HiveTbls hiveTable, Featurestore featurestore, Project project,
    Users user) throws FeaturestoreException {
    List<HiveKeyConstraints> primaryKeys = hiveTable.getHiveKeyConstraintsCollection().stream()
      // 0 is the primary key index
      .filter(c -> c.getConstraintType() == 0)
      .collect(Collectors.toList());

    List<SQLDefaultConstraint> defaultConstraints =
      offlineFeatureGroupController.getDefaultConstraints(featurestore, hiveTable.getTblName(), project, user);

    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS = new ArrayList<>();
    // Add all the columns - if there is a primary key constraint, set the primary key flag
    for (HiveColumns hc : hiveTable.getSdId().getCdId().getHiveColumnsCollection()) {
      boolean primary = primaryKeys.stream().anyMatch(pk ->
        pk.getParentCdId().getCdId().equals(hc.getHiveColumnsPK().getCdId()) &&
          pk.getParentIntegerIdx() == hc.getIntegerIdx());
      String defaultValue = getDefaultValue(defaultConstraints, hc.getHiveColumnsPK().getColumnName());
      featureGroupFeatureDTOS.add(new FeatureGroupFeatureDTO(hc.getHiveColumnsPK().getColumnName(), hc.getTypeName(),
        hc.getComment(), primary, defaultValue));
    }
    // Hive stores the partition columns separately. Add them
    for (HivePartitionKeys pk : hiveTable.getHivePartitionKeysCollection()) {
      String defaultValue = getDefaultValue(defaultConstraints, pk.getHivePartitionKeysPK().getPkeyName());
      featureGroupFeatureDTOS.add(new FeatureGroupFeatureDTO(pk.getHivePartitionKeysPK().getPkeyName(),
        pk.getPkeyType(), pk.getPkeyComment(), false, true, defaultValue));
    }
    return featureGroupFeatureDTOS;
  }

  private String getDefaultValue(List<SQLDefaultConstraint> defaultConstraints, String columnName) {
    return defaultConstraints.stream().filter(constraint -> constraint.getColumn_name().equals(columnName))
      .map(SQLDefaultConstraint::getDefault_value).findAny().orElse(null);
  }

  /**
   * Gets the SQL schema that was used to create the Hive table for a featuregroup
   *
   * @param featuregroup    featuregroup
   * @param project         the project of the user making the request
   * @param user            the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  private FeaturegroupPreview getSQLSchemaForFeaturegroup(Featuregroup featuregroup, Project project, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    String tbl = getTblName(featuregroup.getName(), featuregroup.getVersion());
    String query = "SHOW CREATE TABLE " + tbl;
    String db = featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    return executeReadHiveQuery(query, db, project, user);
  }

  /**
   * Drop a feature group
   * @param featuregroup
   * @param project
   * @param user
   * @throws FeaturestoreException
   * @throws IOException
   * @throws ServiceException
   */
  public void dropHiveFeaturegroup(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, IOException, ServiceException {
    String db = featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    String tableName = getTblName(featuregroup.getName(), featuregroup.getVersion());
    offlineFeatureGroupController.dropFeatureGroup(db, tableName, project, user);
  }

  /**
   * Drops a online feature group in MySQL database
   *
   * @param featuregroup    a cached feature group
   * @param project
   * @param user            the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   */
  public void dropMySQLFeaturegroup(Featuregroup featuregroup, Project project, Users user)
      throws SQLException, FeaturestoreException {
    if(settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().isOnlineEnabled()){
      onlineFeaturegroupController.dropMySQLTable(featuregroup, project, user);
    }
  }

  /**
   * Parses a ResultSet from a Hive query into a list of RowValueQueryResultDTOs
   *
   * @param rs resultset to parse
   * @return list of parsed rows
   * @throws SQLException
   */
  public FeaturegroupPreview parseResultset(ResultSet rs) throws SQLException {
    ResultSetMetaData rsmd = rs.getMetaData();
    FeaturegroupPreview featuregroupPreview = new FeaturegroupPreview();

    while (rs.next()) {
      FeaturegroupPreview.Row row = new FeaturegroupPreview.Row();

      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        Object columnValue = rs.getObject(i);
        row.addValue(new Pair<>(parseColumnLabel(rsmd.getColumnLabel(i)),
            columnValue == null ? null : columnValue.toString()));
      }
      featuregroupPreview.addRow(row);
    }

    return featuregroupPreview;
  }

  /**
   * Column labels contain the table name as well. Remove it
   * @param columnLabel
   * @return
   */
  private String parseColumnLabel(String columnLabel) {
    if (columnLabel.contains(".")) {
      return columnLabel.split("\\.")[1];
    }
    return columnLabel;
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
  private FeaturegroupPreview executeReadHiveQuery(String query, String databaseName, Project project, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    Connection conn = null;
    Statement stmt = null;
    try {
      //Re-create the connection every time since the connection is database and user-specific
      conn = initConnection(databaseName, project, user);
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      return parseResultset(rs);
    } catch (SQLException e) {
      //Hive throws a generic HiveSQLException not a specific AuthorizationException
      if (e.getMessage().toLowerCase().contains("permission denied")) {
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
            "project: " + project.getName() + ", hive database: " + databaseName + " hive query: " + query,
            e.getMessage(), e);
      } else {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HIVE_READ_QUERY_ERROR, Level.SEVERE,
            "project: " + project.getName() + ", hive database: " + databaseName + " hive query: " + query,
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
   * Synchronizes an already created Hive table with the Feature Store metadata
   *
   * @param featurestore the featurestore of the feature group
   * @param cachedFeaturegroupDTO the feature group DTO
   * @return a DTO of the created feature group
   * @throws FeaturestoreException
   */
  public CachedFeaturegroup syncHiveTableWithFeaturestore(Featurestore featurestore,
    CachedFeaturegroupDTO cachedFeaturegroupDTO) throws FeaturestoreException {
  
    //Get Hive Table Metadata
    String tableName = getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    HiveTbls hiveTbls = cachedFeaturegroupFacade.getHiveTableByNameAndDB(tableName, featurestore.getHiveDbId())
        .orElseThrow(() ->new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.SYNC_TABLE_NOT_FOUND, Level.FINE,
        ", tried to sync hive table with name: " + tableName + " with the feature store, but the table was not found " +
          "in the Hive metastore"));

    //Persist cached feature group
    return persistCachedFeaturegroupMetadata(hiveTbls, false, cachedFeaturegroupDTO.getTimeTravelFormat());
  }

  /**
   * Persists metadata of a new cached feature group in the cached_feature_group table
   *
   * @param hiveTable the id of the Hive table in the Hive metastore
   * @return Entity of the created cached feature group
   */
  private CachedFeaturegroup persistCachedFeaturegroupMetadata(HiveTbls hiveTable, boolean onlineEnabled,
                                                               TimeTravelFormat timeTravelFormat) {
    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setHiveTbls(hiveTable);
    cachedFeaturegroup.setOnlineEnabled(onlineEnabled);
    cachedFeaturegroup.setTimeTravelFormat(timeTravelFormat);
    cachedFeaturegroupFacade.persist(cachedFeaturegroup);
    return cachedFeaturegroup;
  }
  
  /**
   * Update a cached featuregroup that currently does not support online feature serving, to support it.
   *
   * @param featurestore the featurestore where the featuregroup resides
   * @param featuregroup the featuregroup entity to update
   * @param user the user making the request
   * @return a DTO of the updated featuregroup
   * @throws FeaturestoreException
   * @throws SQLException
   */
  public FeaturegroupDTO enableFeaturegroupOnline(Featurestore featurestore, Featuregroup featuregroup,
                                                  Project project, Users user)
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
    //Create MySQL Table for Online Feature Group
    String tableName = getTblName(featuregroup.getName(), featuregroup.getVersion());
    List<FeatureGroupFeatureDTO> features =
      getFeaturesDTO(cachedFeaturegroup.getHiveTbls(), featurestore, project, user);
    if(!cachedFeaturegroup.isOnlineEnabled()) {
      onlineFeaturegroupController.createMySQLTable(featurestore, tableName, features, project, user);
    }
    //Set foreign key of the cached feature group to the new online feature group
    cachedFeaturegroup.setOnlineEnabled(true);
    cachedFeaturegroupFacade.updateMetadata(cachedFeaturegroup);
    return convertCachedFeaturegroupToDTO(featuregroup, project, user);
  }
  
  /**
   * Update a cached featuregroup that currently supports online feature serving, and disable it (drop MySQL db)
   *
   * @param featuregroup the featuregroup entity to update
   * @param project
   * @param user the user making the request
   * @return a DTO of the updated featuregroup
   * @throws FeaturestoreException
   * @throws SQLException
   */
  public FeaturegroupDTO disableFeaturegroupOnline(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, SQLException {
    if(!settings.isOnlineFeaturestore()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this Hopsworks cluster.");
    }
    if (!onlineFeaturestoreController.checkIfDatabaseExists(
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()))) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
        Level.FINE, "Online Featurestore is not enabled for this project. To enable online feature store, talk to an " +
        "administrator.");
    }
    CachedFeaturegroup cachedFeaturegroup = featuregroup.getCachedFeaturegroup();
    if (cachedFeaturegroup.isOnlineEnabled()) {
      //Drop MySQL Table for Online Feature Group
      dropMySQLFeaturegroup(featuregroup, project, user);
      cachedFeaturegroup.setOnlineEnabled(false);
      cachedFeaturegroupFacade.persist(cachedFeaturegroup);
    }
    return convertCachedFeaturegroupToDTO(featuregroup, project, user);
  }

  public void updateMetadata(Project project, Users user, Featuregroup featuregroup,
                             CachedFeaturegroupDTO cachedFeaturegroupDTO)
      throws FeaturestoreException, SQLException {
    CachedFeaturegroup cachedFeaturegroup = featuregroup.getCachedFeaturegroup();
    HiveTbls hiveTable = cachedFeaturegroup.getHiveTbls();
    List<FeatureGroupFeatureDTO> previousSchema = getFeaturesDTO(hiveTable, featuregroup.getFeaturestore(), project,
      user);
    String tableName = getTblName(featuregroup.getName(), featuregroup.getVersion());

    // verify user input specific for cached feature groups - if any
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    if (cachedFeaturegroupDTO.getFeatures() != null) {
      verifyPreviousSchemaUnchanged(previousSchema, cachedFeaturegroupDTO.getFeatures());
      newFeatures = verifyAndGetNewFeatures(previousSchema, cachedFeaturegroupDTO.getFeatures());
    }

    if (!Strings.isNullOrEmpty(cachedFeaturegroupDTO.getDescription())) {
      offlineFeatureGroupController.alterHiveTableDescription(
        featuregroup.getFeaturestore(), tableName, cachedFeaturegroupDTO.getDescription(), project, user);
    }

    // alter table
    if (!newFeatures.isEmpty()) {
      offlineFeatureGroupController.alterHiveTableFeatures(
        featuregroup.getFeaturestore(), tableName, newFeatures, project, user);

      // if online feature group
      if (settings.isOnlineFeaturestore() && featuregroup.getCachedFeaturegroup().isOnlineEnabled()) {
        onlineFeaturegroupController.alterMySQLTableColumns(
          featuregroup.getFeaturestore(), tableName, newFeatures, project, user);
      }
    }
  }

  private List<FeatureGroupFeatureDTO> addHudiSpecFeatures(List<FeatureGroupFeatureDTO> features)  {

    for (String hudiSpecFeature : HUDI_SPEC_FEATURE_NAMES){
      // 1st check if hudi specific metadata exists in feature dataframe. If not add
      if (!features.stream().anyMatch(o -> o.getName().equals(hudiSpecFeature))){
        features.add(new FeatureGroupFeatureDTO(hudiSpecFeature, "string",
            "hudi spec metadata feature", false, false));
      }
    }
    return features;
  }

  public List<String> getHudiSpecFeatures(){
    return HUDI_SPEC_FEATURE_NAMES;
  }

  public void verifyPreviousSchemaUnchanged(List<FeatureGroupFeatureDTO> previousSchema,
    List<FeatureGroupFeatureDTO> newSchema) throws FeaturestoreException {
    for (FeatureGroupFeatureDTO feature : previousSchema) {
      FeatureGroupFeatureDTO newFeature =
        newSchema.stream().filter(newFeat -> feature.getName().equals(newFeat.getName())).findAny().orElseThrow(() ->
          new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
            "Feature " + feature.getName() + " was not found in new schema. It is only possible to append features."));
      if (newFeature.getPartition() != feature.getPartition() || newFeature.getPrimary() != feature.getPrimary() ||
        !newFeature.getType().equalsIgnoreCase(feature.getType())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
          "Primary key, partition key or type information of feature " + feature.getName() + " changed. Primary key" +
            ", partition key and type cannot be changed when appending features.");
      }
    }
  }

  public List<FeatureGroupFeatureDTO> verifyAndGetNewFeatures (List<FeatureGroupFeatureDTO> previousSchema,
                                                               List<FeatureGroupFeatureDTO> newSchema)
      throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    for (FeatureGroupFeatureDTO newFeature : newSchema) {
      boolean isNew =
        !previousSchema.stream().anyMatch(previousFeature -> previousFeature.getName().equals(newFeature.getName()));
      if (isNew) {
        newFeatures.add(newFeature);
        if (newFeature.getPrimary() || newFeature.getPartition()) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
            "Appended feature `" + newFeature.getName() + "` is specified as primary or partition key. Primary key and "
              + "partition key cannot be changed when appending features.");
        }
        if (newFeature.getType() == null) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_UPDATE, Level.FINE,
            "Appended feature `" + newFeature.getName() + "` is missing type information. Type information is " +
              "mandatory when appending features to a feature group.");
        }
      }
    }
    return newFeatures;
  }

  public void verifyPrimaryKeyNotPartitionKey(List<FeatureGroupFeatureDTO> features) throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> primaryKeys = features.stream().filter(FeatureGroupFeatureDTO::getPrimary)
      .collect(Collectors.toList());
    for (FeatureGroupFeatureDTO primaryKey : primaryKeys) {
      if(primaryKey.getPartition()){
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP, Level.FINE,
          "The primary key column: " + primaryKey.getName() + " was specified as a partition column, which is not " +
            "allowed.");
      }
    }
  }
}
