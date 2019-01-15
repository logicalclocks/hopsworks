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

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.dao.featurestore.dependencies.FeaturestoreDependencyController;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.FeaturestoreStatisticController;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.FeatureCorrelationMatrixDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionsDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.FeaturestoreException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
  private FeaturestoreDependencyController featurestoreDependencyController;
  @EJB
  private InodeFacade inodeFacade;

  private static final Logger LOGGER = Logger.getLogger(FeaturegroupController.class.getName());
  private static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";

  /**
   * Initializes a JDBC connection (thrift RPC) to HS2 using SSL with a given project user and database
   *
   * @param databaseName name of the Hive database to open a connection to
   * @param project      the project of the user making the request
   * @param user         the user making the request
   * @return conn the JDBC connection
   * @throws SQLException
   * @throws IOException
   */
  private Connection initConnection(String databaseName, Project project, Users user) throws SQLException, IOException,
      FeaturestoreException {
    try {
      // Load Hive JDBC Driver
      Class.forName(HIVE_DRIVER);

      //Check if certs exists, otherwise materialize them
      materializeCerts(project, user);

      //Read password
      String password = new String(Files.readAllBytes(Paths.get(getUserTransientPasswordPath(project, user))));

      // Create connection url
      String hiveEndpoint = settings.getHiveServerHostName(false);
      String jdbcString = "jdbc:hive2://" + hiveEndpoint + "/" + databaseName + ";" +
          "auth=noSasl;ssl=true;twoWay=true;" +
          "sslTrustStore=" + getUserTransientTruststorePath(project, user) + ";" +
          "trustStorePassword=" + password + ";" +
          "sslKeyStore=" + getUserTransientKeystorePath(project, user) + ";" +
          "keyStorePassword=" + password;

      return DriverManager.getConnection(jdbcString);
    } catch (FileNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Could not find user certificates for authenticating with Hive: " +
          e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CERTIFICATES_NOT_FOUND, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "Error initiating Hive connection: " +
          e);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_INITIATE_HIVE_CONNECTION, Level.SEVERE,
          "project: " + project.getName() + ", hive database: " + databaseName, e.getMessage(), e);
    }
  }

  /**
   * Checks if materialized certs for the given user already exists in the tmp dir, otherwise materializes them
   *
   * @param project the project for which the certs will be used
   * @param user    the user for which the certs will be used
   * @throws IOException
   */
  private void materializeCerts(Project project, Users user) throws IOException {
    String pwdPath = getUserTransientKeystorePath(project, user);
    String truststorePath = getUserTransientTruststorePath(project, user);
    String keystorePath = getUserTransientKeystorePath(project, user);
    Boolean fileMissing = false;
    File pwdFile = new File(pwdPath);
    if (!pwdFile.exists())
      fileMissing = true;
    File truststoreFile = new File(truststorePath);
    if (!truststoreFile.exists())
      fileMissing = true;
    File keystoreFile = new File(keystorePath);
    if (!keystoreFile.exists())
      fileMissing = true;
    if (fileMissing)
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
  }

  /**
   * Returns the path to materialized password file for a project-user
   *
   * @param project the project for which the password will be used
   * @param user    the user for which the password will be used
   * @return materialized password path
   */
  private String getUserTransientPasswordPath(Project project, Users user) {
    String transientDir = settings.getHopsworksTmpCertDir();
    String keyName = project.getName() + "__" + user.getUsername() + "__cert.key";
    return transientDir + "/" + keyName;
  }

  /**
   * Returns the path to materialized truststore for a project-user
   *
   * @param project the project for which the truststore will be used
   * @param user    the user for which the truststore will be used
   * @return path to truststore
   */
  private String getUserTransientTruststorePath(Project project, Users user) {
    String transientDir = settings.getHopsworksTmpCertDir();
    String truststoreName = project.getName() + "__" + user.getUsername() + "__tstore.jks";
    return transientDir + "/" + truststoreName;
  }

  /**
   * Returns the path to materialized keystore for a project-user
   *
   * @param project the project for which the keystore will be used
   * @param user    the user for which the keystore will be used
   * @return path to keystore
   */
  private String getUserTransientKeystorePath(Project project, Users user) {
    String transientDir = settings.getHopsworksTmpCertDir();
    String keystoreName = project.getName() + "__" + user.getUsername() + "__kstore.jks";
    return transientDir + "/" + keystoreName;
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
   * @return JSON/XML DTO with the schema
   * @throws IOException
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public RowValueQueryResult getSchema(
      FeaturegroupDTO featuregroupDTO, Project project, Users user,
      Featurestore featurestore)
      throws IOException, SQLException, FeaturestoreException, HopsSecurityException {
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
    List<FeatureDTO> featureDTOs = featuregroupFacade.getHiveFeatures(featuregroup.getHiveTblId());
    String primaryKeyName = featuregroupFacade.getHiveTablePrimaryKey(featuregroup.getHiveTblId());
    featureDTOs.stream().filter(f -> f.getName().equals(primaryKeyName))
        .collect(Collectors.toList()).get(0).setPrimary(true);
    featuregroupDTO.setFeatures(featureDTOs);
    String featuregroupName = featuregroupFacade.getHiveTableName(featuregroup.getHiveTblId());
    int versionLength = featuregroup.getVersion().toString().length();
    //Remove the _version suffix
    featuregroupName = featuregroupName.substring(0, featuregroupName.length() - (1 + versionLength));
    featuregroupDTO.setName(featuregroupName);
    String featurestoreName = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
    featuregroupDTO.setFeaturestoreName(featurestoreName);
    List<String> hdfsStorePaths = featuregroupFacade.getHiveTableHdfsPaths(featuregroup.getHiveTblId());
    featuregroupDTO.setHdfsStorePaths(hdfsStorePaths);
    String docComment = featuregroupFacade.getHiveTableComment(featuregroup.getHiveTblId());
    featuregroupDTO.setDescription(docComment);
    Long inodeId = featuregroupFacade.getFeaturegroupInodeId(featuregroup.getHiveTblId());
    featuregroupDTO.setInodeId(inodeId);
    featuregroupDTO.setDependencies((List) featuregroup.getDependencies(), inodeFacade);
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
   * @return
   */
  private String getTblName(String featuregroupName, Integer version) {
    return featuregroupName + "_" + version.toString();
  }

  /**
   * Creates a new featuregroup: Create Hive Table and store metadata in Hopsworks
   *
   * @param project                  the project of the user making the request
   * @param user                     the user who creates the featuregroup
   * @param featurestore             the featurestore that the featuregroup belongs to
   * @param featuregroupName         the name of the new featuregroup
   * @param features                 string of features separated with comma
   * @param featuregroupDoc          description of the featuregroup
   * @param dependencies             dependencies to create the featuregroup (e.g input datasets to feature engineering)
   * @param job                      (optional) job to compute this feature group\
   * @param version                  version of the featuregroup
   * @param featureCorrelationMatrix feature correlation data
   * @param descriptiveStatistics    descriptive statistics data
   * @param featuresHistogram        feature distributions data
   * @param clusterAnalysis          cluster analysis JSON
   * @return
   * @throws IOException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO createFeaturegroup(
      Project project, Users user, Featurestore featurestore,
      String featuregroupName, String features, String featuregroupDoc,
      List<String> dependencies, Jobs job, Integer version,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix, DescriptiveStatsDTO descriptiveStatistics,
      FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis)
      throws IOException, SQLException, FeaturestoreException, HopsSecurityException {
    //Create Hive Table
    String db = featurestoreController.getFeaturestoreDbName(featurestore.getProject());
    String tableName = getTblName(featuregroupName, version);
    String query = "CREATE TABLE " + db + ".`" + tableName + "`(" +
        features + ")" + " COMMENT '" + featuregroupDoc + "' STORED AS " +
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
    featuregroupFacade.persist(featuregroup);
    featurestoreDependencyController.updateFeaturestoreDependencies(featuregroup, null, dependencies);
    featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null, featureCorrelationMatrix,
        descriptiveStatistics, featuresHistogram, clusterAnalysis);
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
   * Updates metadata about a featuregroup (since only metadata is changed, the Hive table does not need
   * to be modified)
   *
   * @param featurestore             the featurestore where the featuregroup resides
   * @param id                       the id of the featuregroup
   * @param job                      the new job to associate with the featuregroup
   * @param dependencies             the new dependencies to associate with the featuregroup
   * @param featureCorrelationMatrix base64 string with feature correlation matrix
   * @param descriptiveStatistics    JSON string with descriptive statistics
   * @param updateMetadata           boolean flag whether to update featuregroup metadata
   * @param updateStats              boolean flag whether to update featuregroup stats
   * @param featuresHistogram        base64 string with histogram figures for features
   * @param clusterAnalysis          cluster analysis JSON string
   * @return
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO updateFeaturegroupMetadata(
      Featurestore featurestore, Integer id, Jobs job, List<String> dependencies,
      FeatureCorrelationMatrixDTO featureCorrelationMatrix,
      DescriptiveStatsDTO descriptiveStatistics, boolean updateMetadata, boolean updateStats,
      FeatureDistributionsDTO featuresHistogram,
      ClusterAnalysisDTO clusterAnalysis)
      throws FeaturestoreException {
    Featuregroup featuregroup = featuregroupFacade.findByIdAndFeaturestore(id, featurestore);
    if (featuregroup == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.FINE, "featuregroupId: " + id);
    }
    Featuregroup updatedFeaturegroup = featuregroup;
    if (updateMetadata) {
      updatedFeaturegroup =
          featuregroupFacade.updateFeaturegroupMetadata(featuregroup, job);
      featurestoreDependencyController.updateFeaturestoreDependencies(updatedFeaturegroup, null, dependencies);
    }
    if (updateStats) {
      featurestoreStatisticController.updateFeaturestoreStatistics(featuregroup, null, featureCorrelationMatrix,
          descriptiveStatistics, featuresHistogram, clusterAnalysis);
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
   * @throws IOException
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getFeaturegroupPreview(
      FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Project project, Users user)
      throws IOException, SQLException, FeaturestoreException, HopsSecurityException {
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
   * @throws IOException
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<RowValueQueryResult> getSQLSchemaForFeaturegroup(
      FeaturegroupDTO featuregroupDTO,
      Project project, Users user, Featurestore featurestore)
      throws IOException, SQLException, FeaturestoreException, HopsSecurityException {
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
   * @return
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
   * Deletes a featuregroup with a particular id from a particular featurestore
   *
   * @param featurestore the featurestore that the featuregroup belongs to
   * @param id           if of the featuregroup
   * @param project      the project of the user making the request
   * @param user         the user making the request
   * @return JSON/XML DTO of the deleted featuregroup
   * @throws IOException
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public FeaturegroupDTO deleteFeaturegroupWithIdAndFeaturestore(
      Featurestore featurestore, Integer id, Project project, Users user)
      throws IOException, SQLException, FeaturestoreException, HopsSecurityException {
    Featuregroup featuregroup = featuregroupFacade.findByIdAndFeaturestore(id, featurestore);
    if (featuregroup == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.SEVERE,
          "Could not find feature group with id: " + id + " in feature store:" + featurestore +
              " , project: " + project.getName());
    }
    FeaturegroupDTO featuregroupDTO = convertFeaturegrouptoDTO(featuregroup);
    dropFeaturegroup(featuregroupDTO.getName(), featuregroup.getVersion(), project, user, featurestore);
    return featuregroupDTO;
  }

  /**
   * Runs a DROP TABLE statement on a featuregroup of a featurestore hive DB
   *
   * @param featuregroupName name of the featuregroup to delete
   * @param version          version of the featuregroup to delete
   * @param project          the project of the user making the request
   * @param user             the user making the request
   * @param featurestore     the featurestore where the featuregroup resides
   * @throws IOException
   * @throws SQLException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void dropFeaturegroup(
      String featuregroupName, Integer version,
      Project project, Users user, Featurestore featurestore) throws IOException, SQLException,
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
   * @throws IOException
   * @throws FeaturestoreException
   * @throws HopsSecurityException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private void executeUpdateHiveQuery(String query, String databaseName, Project project, Users user)
      throws SQLException, IOException, FeaturestoreException, HopsSecurityException {
    //Re-create the connection every time since the connection is database and user-specific
    Connection conn = initConnection(databaseName, project, user);
    Statement stmt = null;
    try {
      // Create database
      stmt = conn.createStatement();
      stmt.executeUpdate(query);
    } catch (Exception e) {
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
      closeConnection(conn);
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
   * @throws IOException
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  private List<RowValueQueryResult> executeReadHiveQuery(
      String query, String databaseName, Project project, Users user)
      throws SQLException, IOException, FeaturestoreException, HopsSecurityException {
    //Re-create the connection every time since the connection is database and user-specific
    Connection conn = initConnection(databaseName, project, user);
    Statement stmt = null;
    List<RowValueQueryResult> resultList = null;
    try {
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      resultList = parseResultset(rs);
    } catch (Exception e) {
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
      closeConnection(conn);
    }
    return resultList;
  }

  /**
   * Checks if the JDBC connection to HS2 is open, and if so closes it.
   */
  private void closeConnection(Connection conn) {
    try {
      if (conn != null) {
        conn.close();
      }
      //don't add this line:
      //certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      //concurrent requests by the same user will fail, let certs be cleaned up by
      //garbage collector periodically instead
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Error closing Hive JDBC connection: " +
          e);
    }
  }
}
