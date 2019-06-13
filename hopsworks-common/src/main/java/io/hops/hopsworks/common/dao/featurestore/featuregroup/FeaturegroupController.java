/*
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
 */

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticController;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.external_sql_query.FeaturestoreExternalSQLQuery;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.external_sql_query.FeaturestoreExternalSQLQueryFacade;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnectorFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the feature_group table and required business logic
 */
@Stateless
public class FeaturegroupController {
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturestoreStatisticController featurestoreStatisticController;
  @EJB
  private FeaturestoreFeatureController featurestoreFeatureController;
  @EJB
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
  @EJB
  private FeaturestoreExternalSQLQueryFacade featurestoreExternalSQLQueryFacade;

  private static final Logger LOGGER = Logger.getLogger(FeaturegroupController.class.getName());
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
   * Gets all featuregroups for a particular featurestore and project, using the userCerts to query Hive
   *
   * @param featurestore featurestore to query featuregroups for
   * @return list of XML/JSON DTOs of the featuregroups
   */
  public List<FeaturegroupDTO> getFeaturegroupsForFeaturestore(Featurestore featurestore) {
    List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
    return featuregroups.stream().map(fg -> convertFeaturegrouptoDTO(fg)).collect(Collectors.toList());
  }

  /**
   * Executes "SHOW CREATE TABLE" on the hive table of the featuregroup formats it as a string and returns it
   *
   * @param featuregroupDTO the featuregroup to get the schema for
   * @param project         the project of the user making the request
   * @param user            the user making the request
   * @param featurestore    the featurestore where the featuregroup resides
   * @return                JSON/XML DTO with the schema
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public RowValueQueryResult getSchema(
      FeaturegroupDTO featuregroupDTO, Project project, Users user,
      Featurestore featurestore)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    if(featuregroupDTO.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.CANNOT_FETCH_HIVE_SCHEMA_FOR_ON_DEMAND_FEATUREGROUPS,
        Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
    }
    String sqlSchema = parseSqlSchemaResult(getSQLSchemaForFeaturegroup(featuregroupDTO, project, user, featurestore));
    ColumnValueQueryResult column = new ColumnValueQueryResult("schema", sqlSchema);
    List<ColumnValueQueryResult> columns = new ArrayList<>();
    columns.add(column);
    return new RowValueQueryResult(columns);
  }

  /**
   * Converts a featuregroup entity to a Featuregroup DTO
   *
   * @param featuregroup featuregroup entity
   * @return JSON/XML DTO of the featuregroup
   */
  private FeaturegroupDTO convertFeaturegrouptoDTO(Featuregroup featuregroup) {
    FeaturegroupDTO featuregroupDTO = new FeaturegroupDTO(featuregroup);
    
    String featuregroupName = "";
    String featuregroupDescription = "";
    
    // Cached Feature Groups are Stored in Hive, Fetch Hive metadata from the metastore
    if(featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      List<FeatureDTO> featureDTOs = featuregroupFacade.getHiveFeatures(featuregroup.getHiveTblId());
      String primaryKeyName = featuregroupFacade.getHiveTablePrimaryKey(featuregroup.getHiveTblId());
      if(!featureDTOs.isEmpty() && !Strings.isNullOrEmpty(primaryKeyName)){
        featureDTOs.stream().filter(f -> f.getName().equals(primaryKeyName))
          .collect(Collectors.toList()).get(0).setPrimary(true);
      }
      featuregroupDTO.setFeatures(featureDTOs);
      featuregroupName = featuregroupFacade.getHiveTableName(featuregroup.getHiveTblId());
      List<String> hdfsStorePaths = featuregroupFacade.getHiveTableHdfsPaths(featuregroup.getHiveTblId());
      featuregroupDTO.setHdfsStorePaths(hdfsStorePaths);
      featuregroupDescription = featuregroupFacade.getHiveTableComment(featuregroup.getHiveTblId());
      Long inodeId = featuregroupFacade.getFeaturegroupInodeId(featuregroup.getHiveTblId());
      featuregroupDTO.setInodeId(inodeId);
      HiveTableType hiveTableType = featuregroupFacade.getHiveTableType(featuregroup.getHiveTblId());
      featuregroupDTO.setHiveTableType(hiveTableType);
      String hiveInputFormat = featuregroupFacade.getHiveInputFormat(featuregroup.getHiveTblId());
      featuregroupDTO.setInputFormat(hiveInputFormat);
      featuregroupDTO.setLocation(hdfsStorePaths.get(0));
      int versionLength = featuregroup.getVersion().toString().length();
      //Remove the _version suffix
      featuregroupName = featuregroupName.substring(0, featuregroupName.length() - (1 + versionLength));
    } else if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      // On Demand Feature Groups are strored Remotely, fetch metadata from table `feature_store_external_sql_query`
      featuregroupName = featuregroup.getFeaturestoreExternalSQLQuery().getName();
      featuregroupDescription = featuregroup.getFeaturestoreExternalSQLQuery().getDescription();
    }
    //Common metadata
    featuregroupDTO.setName(featuregroupName);
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    featuregroupDTO.setFeaturestoreName(featurestoreName);
    featuregroupDTO.setDescription(featuregroupDescription);
    return featuregroupDTO;
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
   * Creates a new cached featuregroup: Create Hive Table and store metadata in Hopsworks
   *
   * @param project                  the project of the user making the request
   * @param user                     the user who creates the featuregroup
   * @param featurestore             the featurestore that the featuregroup belongs to
   * @param featuregroupName         the name of the new featuregroup
   * @param features                 string of features separated with comma
   * @param job                      (optional) job to compute this feature group
   * @param version                  version of the featuregroup
   * @param featureCorrelationMatrix feature correlation data
   * @param descriptiveStatistics    descriptive statistics data
   * @param featuresHistogram        feature distributions data
   * @param clusterAnalysis          cluster analysis JSON
   * @return                         a DTO representing the created featuregroup
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO createCachedFeaturegroup(
      Project project, Users user, Featurestore featurestore,
      String featuregroupName, String features, Jobs job, Integer version,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
      FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    //Create Hive Table
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(featuregroupName, version);
    String query = "CREATE TABLE " + db + ".`" + tableName + "` " +
        features + " STORED AS " +
        settings.getFeaturestoreDbDefaultStorageFormat();
    executeUpdateHiveQuery(query, db, project, user);
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    //Get HiveTblId of the newly created table from the metastore
    Long hiveTblId = featuregroupFacade.getHiveTableId(tableName, featurestore.getHiveDbId());

    //Store featuregroup metadata in Hopsworks
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setHdfsUserId(hdfsUser.getId());
    featuregroup.setJob(job);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(version);
    featuregroup.setHiveTblId(hiveTblId);
    featuregroup.setFeaturestoreExternalSQLQuery(null);
    featuregroup.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
    featuregroupFacade.persist(featuregroup);
    
    // Store statistics
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
      featureCorrelationMatrix, descriptiveStatistics, featuresHistogram, clusterAnalysis);
    return convertFeaturegrouptoDTO(featuregroup);
  }
  
  /**
   * Create a new On-Demand Feature Group. Store featuregroup metadata and On-Demand SQL query with JDBC Connector.
   *
   * @param project                          the project of the user making the request
   * @param user                             the user who creates the featuregroup
   * @param featurestore                     the featurestore that the featuregroup belongs to
   * @param featuregroupName                 the name of the new featuregroup
   * @param features                         string of features separated with comma
   * @param job                              (optional) job to compute this feature group
   * @param version                          version of the featuregroup
   * @param featureCorrelationMatrix         feature correlation data
   * @param descriptiveStatistics            descriptive statistics data
   * @param featuresHistogram                feature distributions data
   * @param clusterAnalysis                  cluster analysis JSON
   * @param jdbcConnectorId                  id of the featurestore jdbc connector
   * @param sqlQuery                         the on-demand SQL query
   * @param description                      description of the feature group
   * @return                                 DTO of the created feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO createOnDemandFeaturegroup(
    Project project, Users user, Featurestore featurestore,
    String featuregroupName, List<FeatureDTO> features, Jobs job, Integer version,
    FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
    FeatureDistributionsDTO featuresHistogram,
    ClusterAnalysisDTO clusterAnalysis, Integer jdbcConnectorId, String sqlQuery, String description)
    throws FeaturestoreException {
    
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    
    //Get JDBC Connector
    FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(jdbcConnectorId);
    if(featurestoreJdbcConnector == null){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ON_DEMAND_FEATUREGROUP_JDBC_CONNECTOR_NOT_FOUND,
        Level.FINE, "jdbConnectorId: " + jdbcConnectorId);
    }
    
    //Store external SQL Query metadata in Hopsworks
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery = new FeaturestoreExternalSQLQuery();
    featurestoreExternalSQLQuery.setQuery(sqlQuery);
    featurestoreExternalSQLQuery.setName(featuregroupName);
    featurestoreExternalSQLQuery.setDescription(description);
    featurestoreExternalSQLQuery.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    featurestoreExternalSQLQueryFacade.persist(featurestoreExternalSQLQuery);
  
    //Store schema of OnDemand Feature Group (since we don't have Hive metastore for this now)
    featurestoreFeatureController.updateExternalFgFeatures(featurestoreExternalSQLQuery, features);
  
    //Store featuregroup metadata in Hopsworks
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setHdfsUserId(hdfsUser.getId());
    featuregroup.setJob(job);
    featuregroup.setCreated(new Date());
    featuregroup.setCreator(user);
    featuregroup.setVersion(version);
    featuregroup.setHiveTblId(null);
    featuregroup.setFeaturestoreExternalSQLQuery(featurestoreExternalSQLQuery);
    featuregroup.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    featuregroupFacade.persist(featuregroup);
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
      featureCorrelationMatrix, descriptiveStatistics, featuresHistogram, clusterAnalysis);
    return convertFeaturegrouptoDTO(featuregroup);
  }

  /**
   * Retrieves a featuregroup with a particular id from a particular featurestore
   *
   * @param id           if of the featuregroup
   * @param featurestore the featurestore that the featuregroup belongs to
   * @return XML/JSON DTO of the featuregroup
   */
  public FeaturegroupDTO getFeaturegroupWithIdAndFeaturestore(Featurestore featurestore, Integer id)
      throws FeaturestoreException {
    Featuregroup featuregroup = featuregroupFacade.findByIdAndFeaturestore(id, featurestore);
    if (featuregroup == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.FINE, "featuregroupId: " + id);
    }
    return convertFeaturegrouptoDTO(featuregroup);
  }

  /**
   * Updates metadata about a cached featuregroup (since only metadata is changed, the Hive table does not need
   * to be modified)
   *
   * @param featurestore             the featurestore where the featuregroup resides
   * @param id                       the id of the featuregroup
   * @param job                      the new job to associate with the featuregroup
   * @param featureCorrelationMatrix base64 string with feature correlation matrix
   * @param descriptiveStatistics    JSON string with descriptive statistics
   * @param updateMetadata           boolean flag whether to update featuregroup metadata
   * @param updateStats              boolean flag whether to update featuregroup stats
   * @param featuresHistogram        base64 string with histogram figures for features
   * @param clusterAnalysis          cluster analysis JSON string
   * @return DTO of the updated feature group
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupMetadata(
      Featurestore featurestore, Integer id, Jobs job,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, boolean updateMetadata, boolean updateStats,
      FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis, String featuregroupName, String description, Integer jdbcConnectorId,
    String sqlQuery, List<FeatureDTO> featureDTOS)
      throws FeaturestoreException {
    Featuregroup featuregroup = featuregroupFacade.findByIdAndFeaturestore(id, featurestore);
    if (featuregroup == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.FINE, "featuregroupId: " + id);
    }
    Featuregroup updatedFeaturegroup = featuregroup;
    if (updateMetadata) {
      if(featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP){
        updatedFeaturegroup =
          featuregroupFacade.updateFeaturegroupMetadata(featuregroup, job);
      } else if(featuregroup.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP){
        verifyOnDemandFeaturegroupUserInput(featuregroupName, description, jdbcConnectorId, sqlQuery, featureDTOS);
        FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery = featuregroup.getFeaturestoreExternalSQLQuery();
        FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(jdbcConnectorId);
        featurestoreExternalSQLQueryFacade.updateMetadata(featurestoreExternalSQLQuery, featuregroupName,
          description, featurestoreJdbcConnector, sqlQuery);
        featurestoreFeatureController.updateExternalFgFeatures(featurestoreExternalSQLQuery, featureDTOS);
      }
    }
    if (updateStats && featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null,
        featureCorrelationMatrix, descriptiveStatistics, featuresHistogram, clusterAnalysis);
    }
    return convertFeaturegrouptoDTO(updatedFeaturegroup);
  }

  /**
   * Previews a given featuregroup by doing a SELECT LIMIT query on the Hive Table
   *
   * @param featuregroupDTO DTO of the featuregroup to preview
   * @param project         the project of the user making the request
   * @param featurestore    the feature store where the feature group resides
   * @param user            the user making the request
   * @return list of feature-rows from the Hive table where the featuregroup is stored
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getFeaturegroupPreview(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Project project, Users user)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    if(featuregroupDTO.getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.PREVIEW_NOT_SUPPORTED_FOR_ON_DEMAND_FEATUREGROUPS,
        Level.FINE, "featuregroupId: " + featuregroupDTO.getId());
    }
    String tbl = getTblName(featuregroupDTO.getName(), featuregroupDTO.getVersion());
    String query = "SELECT * FROM " + tbl + " LIMIT 20";
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    return executeReadHiveQuery(query, db, project, user);
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
   * Gets a featuregroup in a specific project and featurestore with the given name and version
   *
   * @param project          the project of the user making the request
   * @param featurestore     the featurestore where the featuregroup resides
   * @param featuregroupName the name of the featuregroup
   * @param version          version of the featuregroup
   * @return DTO of the featuregroup
   * @throws FeaturestoreException
   */
  public FeaturegroupDTO getFeaturegroupByFeaturestoreAndName(
      Project project, Featurestore featurestore, String featuregroupName, int version) throws FeaturestoreException {
    List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
    List<FeaturegroupDTO> featuregroupDTOS =
        featuregroups.stream().map(fg -> convertFeaturegrouptoDTO(fg)).collect(Collectors.toList());
    List<FeaturegroupDTO> featuregroupsDTOWithName =
        featuregroupDTOS.stream().filter(fg -> fg.getName().equals(featuregroupName) &&
            fg.getVersion().intValue() == version)
            .collect(Collectors.toList());
    if (featuregroupsDTOWithName.size() != 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.FINE, "featurestoreId: " + featurestore.getId() + " , project: " + project.getName() +
          " featuregroupName: " + featuregroupName);
    }
    //Featuregroup name corresponds to Hive table inside the featurestore so uniqueness is enforced by Hive
    return featuregroupsDTOWithName.get(0);
  }

  /**
   * Deletes a featuregroup with a particular id or name from a featurestore
   *
   * @param featurestore                   the featurestore that the featuregroup belongs to
   * @param id                             if of the featuregroup
   * @param project                        the project of the user making the request
   * @param user                           the user making the request
   * @param featuregroupName               the name of the featuregroup
   * @param featuregroupVersion            the version of the featuregroup
   * @return JSON/XML DTO of the deleted featuregroup
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO deleteFeaturegroupIfExists(
      Featurestore featurestore, Integer id, Project project, Users user,
    String featuregroupName, Integer featuregroupVersion)
      throws SQLException, FeaturestoreException, HopsSecurityException {
    Featuregroup featuregroup = null;
    if(id != null && featurestore != null){
      featuregroup = featuregroupFacade.findByIdAndFeaturestore(id, featurestore);
      if (featuregroup == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.SEVERE,
          "Could not find feature group with id: " + id + " in feature store:" + featurestore +
            " , project: " + project.getName());
      }
    } else {
      if(id == null){
        List<Featuregroup> featuregroups = featuregroupFacade.findByFeaturestore(featurestore);
        featuregroups = featuregroups.stream().filter(fg -> {
          FeaturegroupDTO featuregroupDTO = convertFeaturegrouptoDTO(fg);
          return featuregroupDTO.getName().equals(featuregroupName) &&
            featuregroupDTO.getVersion() == featuregroupVersion;
        }).collect(Collectors.toList());
        if(!featuregroups.isEmpty())
          featuregroup = featuregroups.get(0);
      } else {
        featuregroup = featuregroupFacade.findById(id);
      }
    }
    if(featuregroup != null){
      FeaturegroupDTO featuregroupDTO = convertFeaturegrouptoDTO(featuregroup);
      if(featuregroupDTO.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP){
        dropHiveFeaturegroup(featuregroupDTO.getName(), featuregroup.getVersion(), project, user, featurestore);
      } else {
        featuregroupFacade.remove(featuregroup);
      }
      return featuregroupDTO;
    } else {
      return null;
    }
  }

  /**
   * Runs a DROP TABLE statement on a featuregroup of a featurestore hive DB
   *
   * @param featuregroupName name of the featuregroup to delete
   * @param version          version of the featuregroup to delete
   * @param project          the project of the user making the request
   * @param user             the user making the request
   * @param featurestore     the featurestore where the featuregroup resides
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void dropHiveFeaturegroup(
      String featuregroupName, Integer version,
      Project project, Users user, Featurestore featurestore) throws SQLException,
      FeaturestoreException, HopsSecurityException {
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(featuregroupName, version);
    String query = "DROP TABLE IF EXISTS `" + tableName + "`";
    executeUpdateHiveQuery(query, db, project, user);
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
   * Verify user input specific for creation of on-demand feature group
   *
   * @param featuregroupName name of the feature group
   * @param description description of the feature group
   * @param jdbcConnectorId JDBC connector
   * @param features list of features for the feature group
   * @param sqlQuery SQL Query
   */
  public void verifyOnDemandFeaturegroupUserInput(String featuregroupName, String description, Integer jdbcConnectorId,
    String sqlQuery, List<FeatureDTO> features) {
  
    if(featuregroupName.length() > Settings.HOPS_ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
        + ", the name of a on-demand feature group should be less than "
        + Settings.HOPS_ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH + " characters");
    }
  
    if(!Strings.isNullOrEmpty(description) &&
      description.length() > Settings.HOPS_ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_DESCRIPTION.getMessage()
        + ", the descritpion of an on-demand feature group should be less than "
        + Settings.HOPS_ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH + " " +
        "characters");
    }
  
    features.stream().forEach(f -> {
      if(Strings.isNullOrEmpty(f.getName()) || f.getName().length() >
        Settings.HOPS_ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH){
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME.getMessage()
          + ", the feature name in an on-demand feature group should be less than "
          + Settings.HOPS_ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH + " characters");
      }
      if(!Strings.isNullOrEmpty(f.getDescription()) &&
        f.getDescription().length() > Settings.HOPS_ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION.getMessage()
          + ", the feature description in an on-demand feature group should be less than "
          + Settings.HOPS_ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH + " characters");
      }
    });
    
    if(jdbcConnectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(jdbcConnectorId);
    if(featurestoreJdbcConnector == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND.getMessage()
        + "JDBC connector with id: " + jdbcConnectorId + " was not found");
    }
    
    if(Strings.isNullOrEmpty(sqlQuery)){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY.getMessage()
        + ", SQL Query cannot be empty");
    }
  
    if(sqlQuery.length() > Settings.HOPS_ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY.getMessage()
        + ", SQL Query cannot exceed " + Settings.HOPS_ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH + " characters.");
    }
  }
  
  /**
   * Verify user input specific for creation of on-demand training dataset
   *
   * @param featuregroupName name of the feature group
   * @param description description of the feature group
   * @param featureDTOS list of features for the feature group
   */
  public void verifyCachedFeaturegroupUserInput(String featuregroupName, String description,
    List<FeatureDTO> featureDTOS) {
    
    Pattern namePattern = Pattern.compile(Settings.HOPS_FEATURESTORE_REGEX);
  
    if(featuregroupName.length() > Settings.HOPS_CACHED_FEATUREGROUP_NAME_MAX_LENGTH ||
      !namePattern.matcher(featuregroupName).matches()) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
        + ", the name of a cached feature group should be less than "
        + Settings.HOPS_CACHED_FEATUREGROUP_NAME_MAX_LENGTH + " characters and match " +
        "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
    }
    
    if(!Strings.isNullOrEmpty(description) && description.length() >
      Settings.HOPS_CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_DESCRIPTION.getMessage()
        + ", the descritpion of a cached feature group should be less than "
        + Settings.HOPS_CACHED_FEATUREGROUP_DESCRIPTION_MAX_LENGTH + " characters");
    }
    
    featureDTOS.stream().forEach(f -> {
      if(Strings.isNullOrEmpty(f.getName()) || !namePattern.matcher(f.getName()).matches() || f.getName().length() >
        Settings.HOPS_CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH){
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME.getMessage()
          + ", the feature name in a cached feature group should be less than "
          + Settings.HOPS_CACHED_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH + " characters and match " +
          "the regular expression: " +  Settings.HOPS_FEATURESTORE_REGEX);
      }
      if(!Strings.isNullOrEmpty(f.getDescription()) &&
        f.getDescription().length() > Settings.HOPS_CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION.getMessage()
          + ", the feature description in a cached feature group should be less than "
          + Settings.HOPS_CACHED_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH + " characters");
      }
    });
  }
}
