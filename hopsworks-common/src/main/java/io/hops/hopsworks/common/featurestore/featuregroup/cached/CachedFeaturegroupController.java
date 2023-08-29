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
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeatureGroupInputValidation;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeatureExtraConstraints;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private CachedFeaturegroupFacade cachedFeatureGroupFacade;
  @EJB
  private FeaturegroupFacade featureGroupFacade;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @EJB
  private OfflineFeatureGroupController offlineFeatureGroupController;
  @EJB
  private HiveController hiveController;
  @EJB
  private ConstructorController constructorController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureGroupInputValidation featureGroupInputValidation;

  private static final Logger LOGGER = Logger.getLogger(CachedFeaturegroupController.class.getName());
  private static final List<String> HUDI_SPEC_FEATURE_NAMES = Arrays.asList("_hoodie_record_key",
      "_hoodie_partition_path", "_hoodie_commit_time", "_hoodie_file_name", "_hoodie_commit_seqno");

  @PostConstruct
  public void init() {
    try {
      // Load Hive JDBC Driver
      Class.forName(HiveController.HIVE_DRIVER);
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Could not load the Hive driver: " + HiveController.HIVE_DRIVER, e);
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

      String jdbcString = HiveController.HIVE_JDBC_PREFIX + hiveEndpoint + "/" + databaseName + ";" +
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
    String tbl = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());

    List<FeatureGroupFeatureDTO> features =  featuregroupController.getFeatures(featuregroup, project, user);

    // This is not great, but at the same time the query runs as the user.
    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (FeatureGroupFeatureDTO feature : features) {
      if (feature.getDefaultValue() == null) {
        selectList.add(new SqlIdentifier(Arrays.asList("`" + tbl + "`", "`" + feature.getName() + "`"),
          SqlParserPos.ZERO));
      } else {
        selectList.add(constructorController.selectWithDefaultAs(new Feature(feature, tbl), false));
      }
    }

    SqlNode whereClause = getWhereCondition(partition, features);

    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList,
      new SqlIdentifier("`" + tbl + "`", SqlParserPos.ZERO),
      whereClause, null, null, null, null, null,
      SqlLiteral.createExactNumeric(String.valueOf(limit), SqlParserPos.ZERO), null);
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
  public CachedFeaturegroup createCachedFeaturegroup(Featurestore featurestore,
                                                     CachedFeaturegroupDTO cachedFeaturegroupDTO, Project project,
                                                     Users user)
      throws FeaturestoreException {
    verifyPrimaryKey(cachedFeaturegroupDTO, cachedFeaturegroupDTO.getTimeTravelFormat());

    //Prepare DDL statement
    String tbl = featuregroupController.getTblName(cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion());
    offlineFeatureGroupController.createHiveTable(featurestore, tbl,
        cachedFeaturegroupDTO.getTimeTravelFormat() == TimeTravelFormat.HUDI ?
            addHudiSpecFeatures(cachedFeaturegroupDTO.getFeatures()) :
            cachedFeaturegroupDTO.getFeatures(),
        project, user, getTableFormat(cachedFeaturegroupDTO.getTimeTravelFormat()));

    //Persist cached feature group
    return persistCachedFeaturegroupMetadata(cachedFeaturegroupDTO.getTimeTravelFormat(),
            cachedFeaturegroupDTO.getFeatures());
  }

  /**
   * Converts a CachedFeaturegroup entity into a DTO representation
   *
   * @param featuregroup the entity to convert
   * @return the converted DTO representation
   */
  public CachedFeaturegroupDTO convertCachedFeaturegroupToDTO(Featuregroup featuregroup, Project project, Users user)
      throws ServiceException {
    CachedFeaturegroupDTO cachedFeaturegroupDTO = new CachedFeaturegroupDTO(featuregroup);

    if (settings.isOnlineFeaturestore() && featuregroup.isOnlineEnabled()) {
      cachedFeaturegroupDTO.setOnlineEnabled(true);
      cachedFeaturegroupDTO.setOnlineTopicName(onlineFeaturegroupController
              .onlineFeatureGroupTopicName(project.getId(), featuregroup.getId(),
                Utils.getFeaturegroupName(featuregroup)));
    }
    cachedFeaturegroupDTO.setName(featuregroup.getName());
    cachedFeaturegroupDTO.setTimeTravelFormat(featuregroup.getCachedFeaturegroup().getTimeTravelFormat());
    cachedFeaturegroupDTO.setDescription(featuregroup.getDescription());

    cachedFeaturegroupDTO.setLocation(
            featurestoreUtils.resolveLocation(featuregroupController.getFeatureGroupLocation(featuregroup)));
    return cachedFeaturegroupDTO;
  }
  
  public List<FeatureGroupFeatureDTO> getFeaturesDTO(Featuregroup featuregroup, Project project, Users user)
          throws FeaturestoreException {
    Set<String> primaryKeys = featuregroup.getCachedFeaturegroup().getFeaturesExtraConstraints().stream()
        .filter(CachedFeatureExtraConstraints::getPrimary)
        .map(CachedFeatureExtraConstraints::getName)
        .collect(Collectors.toSet());

    Set<String> precombineKeys = featuregroup.getCachedFeaturegroup().getFeaturesExtraConstraints().stream()
        .filter(CachedFeatureExtraConstraints::getHudiPrecombineKey)
        .map(CachedFeatureExtraConstraints::getName)
        .collect(Collectors.toSet());

    Map<String, String> featureDescription = featuregroup.getCachedFeaturegroup().getCachedFeatures().stream()
        .collect(Collectors.toMap(CachedFeature::getName, CachedFeature::getDescription));

    List<FeatureGroupFeatureDTO> featureGroupFeatures = offlineFeatureGroupController.getSchema(
        featuregroup.getFeaturestore(),
        featuregroupController.getTblName(featuregroup),
        project, user);

    for (FeatureGroupFeatureDTO feature : featureGroupFeatures) {
      feature.setPrimary(primaryKeys.contains(feature.getName()));
      feature.setHudiPrecombineKey(precombineKeys.contains(feature.getName()));
      feature.setDescription(featureDescription.get(feature.getName()));
      feature.setFeatureGroupId(featuregroup.getId());
    }

    if (featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI){
      featureGroupFeatures = dropHudiSpecFeatureGroupFeature(featureGroupFeatures);
    }

    return featureGroupFeatures;
  }

  public List<FeatureGroupFeatureDTO> getFeaturesDTOOnlineChecked(Featuregroup featuregroup,
                                                                  Project project, Users user)
          throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS = getFeaturesDTO(featuregroup, project, user);
    if (settings.isOnlineFeaturestore() && featuregroup.isOnlineEnabled()) {
      featureGroupFeatureDTOS = onlineFeaturegroupController.getFeaturegroupFeatures(featuregroup,
          featureGroupFeatureDTOS);
    }
    return featureGroupFeatureDTOS;
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
  public void deleteFeatureGroup(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, IOException, ServiceException {
    // Drop the table from Hive
    String db = featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());
    offlineFeatureGroupController.dropFeatureGroup(db, tableName, project, user);

    // remove the metadata from the Hopsworks schema
    cachedFeatureGroupFacade.remove(featuregroup.getCachedFeaturegroup());
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
   * Persists metadata of a new cached feature group in the cached_feature_group table
   *
   * @param timeTravelFormat time travel format
   * @param featureGroupFeatureDTOS the list of the feature group feature DTOs
   * @return Entity of the created cached feature group
   */
  private CachedFeaturegroup persistCachedFeaturegroupMetadata(TimeTravelFormat timeTravelFormat,
                                                               List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS) {
    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setTimeTravelFormat(timeTravelFormat);
    cachedFeaturegroup.setFeaturesExtraConstraints(
        buildFeatureExtraConstrains(featureGroupFeatureDTOS, cachedFeaturegroup, null));
    cachedFeaturegroup.setCachedFeatures(featureGroupFeatureDTOS.stream()
      // right now we are filtering and saving only features with description separately, if we add more
      // information to the cached_feature table in the future, we might want to change this and persist a row for
      // every feature
      .filter(feature -> feature.getDescription() != null)
      .map(feature -> new CachedFeature(cachedFeaturegroup, feature.getName(), feature.getDescription()))
      .collect(Collectors.toList()));
    cachedFeatureGroupFacade.persist(cachedFeaturegroup);
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
  public void enableFeaturegroupOnline(Featurestore featurestore, Featuregroup featuregroup,
                                                  Project project, Users user)
      throws FeaturestoreException, SQLException, ServiceException, KafkaException, SchemaException, ProjectException,
      UserException, IOException, HopsSecurityException {
    CachedFeaturegroup cachedFeaturegroup = featuregroup.getCachedFeaturegroup();

    List<FeatureGroupFeatureDTO> features = getFeaturesDTO(featuregroup, project, user);
    if (cachedFeaturegroup.getTimeTravelFormat() == TimeTravelFormat.HUDI){
      features = dropHudiSpecFeatureGroupFeature(features);
    }

    if(!featuregroup.isOnlineEnabled()) {
      onlineFeaturegroupController.setupOnlineFeatureGroup(featurestore, featuregroup, features, project, user);
    }
    //Set foreign key of the cached feature group to the new online feature group
    featuregroup.setOnlineEnabled(true);
    featureGroupFacade.updateFeaturegroupMetadata(featuregroup);
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
  public void disableFeaturegroupOnline(Featuregroup featuregroup, Project project, Users user)
      throws FeaturestoreException, SQLException, SchemaException, KafkaException {
    if (settings.isOnlineFeaturestore() && featuregroup.isOnlineEnabled()) {
      onlineFeaturegroupController.disableOnlineFeatureGroup(featuregroup, project, user);
      featuregroup.setOnlineEnabled(false);
      featureGroupFacade.updateFeaturegroupMetadata(featuregroup);
    }
  }

  public void updateMetadata(Project project, Users user, Featuregroup featuregroup,
    FeaturegroupDTO featuregroupDTO)
    throws FeaturestoreException, SQLException, SchemaException, KafkaException {

    List<FeatureGroupFeatureDTO> previousSchema = getFeaturesDTO(featuregroup, project, user);

    String tableName = featuregroupController.getTblName(featuregroup.getName(), featuregroup.getVersion());

    // verify user input specific for cached feature groups - if any
    List<FeatureGroupFeatureDTO> newFeatures = new ArrayList<>();
    if (featuregroupDTO.getFeatures() != null) {
      verifyPreviousSchemaUnchanged(previousSchema, featuregroupDTO.getFeatures());
      newFeatures = featureGroupInputValidation.verifyAndGetNewFeatures(previousSchema, featuregroupDTO.getFeatures());
    }

    // change feature descriptions
    updateCachedDescriptions(featuregroup.getCachedFeaturegroup(), featuregroupDTO.getFeatures());

    // alter table for new additional features
    if (!newFeatures.isEmpty()) {
      offlineFeatureGroupController.alterHiveTableFeatures(
        featuregroup.getFeaturestore(), tableName, newFeatures, project, user);

      // if online feature group
      if (settings.isOnlineFeaturestore() && featuregroup.isOnlineEnabled()) {
        onlineFeaturegroupController.alterOnlineFeatureGroupSchema(
          featuregroup, newFeatures, featuregroupDTO.getFeatures(), project, user);
      }

      // Log schema change
      String newFeaturesStr = "New features: " + newFeatures.stream().map(FeatureGroupFeatureDTO::getName)
          .collect(Collectors.joining(","));
      fsActivityFacade.logMetadataActivity(user, featuregroup, FeaturestoreActivityMeta.FG_ALTERED, newFeaturesStr);
    }
  }

  private void updateCachedDescriptions(CachedFeaturegroup cachedFeatureGroup,
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOs) {
    for (FeatureGroupFeatureDTO feature : featureGroupFeatureDTOs) {
      Optional<CachedFeature> previousCachedFeature =
        getCachedFeature(cachedFeatureGroup.getCachedFeatures(), feature.getName());
      
      if (feature.getDescription() != null) {
        if (previousCachedFeature.isPresent()) {
          previousCachedFeature.get().setDescription(feature.getDescription());
        } else {
          cachedFeatureGroup.getCachedFeatures().add(new CachedFeature(cachedFeatureGroup, feature.getName(),
            feature.getDescription()));
        }
      }
    }
    cachedFeatureGroupFacade.updateMetadata(cachedFeatureGroup);
  }
  
  public Optional<CachedFeature> getCachedFeature(Collection<CachedFeature> cachedFeatures, String featureName) {
    return cachedFeatures.stream().filter(feature -> feature.getName().equalsIgnoreCase(featureName)).findAny();
  }

  public List<FeatureGroupFeatureDTO> addHudiSpecFeatures(List<FeatureGroupFeatureDTO> features)  {

    for (String hudiSpecFeature : HUDI_SPEC_FEATURE_NAMES){
      // 1st check if hudi specific metadata exists in feature dataframe. If not add
      if (features.stream().noneMatch(o -> o.getName().equals(hudiSpecFeature))){
        features.add(new FeatureGroupFeatureDTO(hudiSpecFeature, "string",
            "hudi spec metadata feature", false, false));
      }
    }
    return features;
  }

  public List<FeatureGroupFeatureDTO> dropHudiSpecFeatureGroupFeature(List<FeatureGroupFeatureDTO> features) {
    return features.stream()
        .filter(feature -> !HUDI_SPEC_FEATURE_NAMES.contains(feature.getName())).collect(Collectors.toList());
  }

  public List<Feature> dropHudiSpecFeatures(List<Feature> features) {
    return features.stream()
        .filter(feature -> !HUDI_SPEC_FEATURE_NAMES.contains(feature.getName())).collect(Collectors.toList());
  }

  private OfflineFeatureGroupController.Formats getTableFormat(TimeTravelFormat timeTravelFormat) {
    switch (timeTravelFormat) {
      case HUDI:
        return OfflineFeatureGroupController.Formats.HUDI;
      default:
        return OfflineFeatureGroupController.Formats.valueOf(settings.getFeaturestoreDbDefaultStorageFormat());
    }
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

  public void verifyPrimaryKey(FeaturegroupDTO featuregroupDTO,
                               TimeTravelFormat timeTravelFormat) throws FeaturestoreException {
    // Currently the Hudi implementation requires having at last one primary key
    if (timeTravelFormat == TimeTravelFormat.HUDI &&
        (featuregroupDTO.getFeatures().stream().noneMatch(FeatureGroupFeatureDTO::getPrimary)))
    {
      if (featuregroupDTO instanceof StreamFeatureGroupDTO) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.PRIMARY_KEY_REQUIRED, Level.FINE, "Stream " +
          "enabled feature groups only support `HUDI` time travel format, which requires a primary key to be set.");
      } else {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.PRIMARY_KEY_REQUIRED, Level.FINE, "Time " +
          "travel format `HUDI` requires a primary key to be set.");
      }
    }
  }

  public List<CachedFeatureExtraConstraints> buildFeatureExtraConstrains(
      List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS, CachedFeaturegroup cachedFeaturegroup,
    StreamFeatureGroup streamFeatureGroup) {
    List<CachedFeatureExtraConstraints> cachedFeatureExtraConstraints = new ArrayList<>();
    List<String> pkNames = featureGroupFeatureDTOS.stream()
        .filter(FeatureGroupFeatureDTO::getPrimary)
        .map(FeatureGroupFeatureDTO::getName)
        .collect(Collectors.toList());

    //hudi precombine key is always one feature
    String hudiPrecombineKeyName = featureGroupFeatureDTOS.stream()
        .filter(FeatureGroupFeatureDTO::getHudiPrecombineKey)
        .map(FeatureGroupFeatureDTO::getName)
        .findFirst().orElse(null);

    boolean primaryKeyIsHudiPrecombineKey = false;

    if (streamFeatureGroup != null || cachedFeaturegroup.getTimeTravelFormat() == TimeTravelFormat.HUDI) {
      if (hudiPrecombineKeyName == null) {
        //hudi precombine key is always one feature, we pick up 1st primary key
        hudiPrecombineKeyName = pkNames.get(0);
        primaryKeyIsHudiPrecombineKey = true;
      } else {
        // User may set primary key as precombine key
        primaryKeyIsHudiPrecombineKey = pkNames.contains(hudiPrecombineKeyName);
      }
    }

    for (String pkName : pkNames) {
      cachedFeatureExtraConstraints.add(streamFeatureGroup == null ?
        new CachedFeatureExtraConstraints(cachedFeaturegroup, pkName, true, pkName.equals(hudiPrecombineKeyName)) :
        new CachedFeatureExtraConstraints(streamFeatureGroup, pkName, true, pkName.equals(hudiPrecombineKeyName)));
    }

    if (!primaryKeyIsHudiPrecombineKey && (cachedFeaturegroup.getTimeTravelFormat() == TimeTravelFormat.HUDI ||
      streamFeatureGroup != null)) {
      cachedFeatureExtraConstraints.add(
        streamFeatureGroup == null ?
          new CachedFeatureExtraConstraints(cachedFeaturegroup, hudiPrecombineKeyName, false, true) :
          new CachedFeatureExtraConstraints(streamFeatureGroup, hudiPrecombineKeyName, false, true) );
    }
    return cachedFeatureExtraConstraints;
  }
}
