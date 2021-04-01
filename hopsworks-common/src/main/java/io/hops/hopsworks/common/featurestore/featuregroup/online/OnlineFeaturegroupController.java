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

package io.hops.hopsworks.common.featurestore.featuregroup.online;

import com.google.common.base.Strings;
import com.logicalclocks.shaded.org.apache.commons.lang3.StringUtils;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.kafka.schemas.SubjectDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreFacade;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.kafka.SchemasController;
import io.hops.hopsworks.common.kafka.SubjectsCompatibilityController;
import io.hops.hopsworks.common.kafka.SubjectsController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.kafka.schemas.SchemaCompatibility;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the online_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OnlineFeaturegroupController {

  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private OnlineFeaturestoreFacade onlineFeaturestoreFacade;
  @EJB
  private Settings settings;
  @EJB
  private SubjectsController subjectsController;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private AvroSchemaConstructorController avroSchemaConstructorController;
  @EJB
  private SchemasController schemasController;
  @EJB
  private SubjectsCompatibilityController subjectsCompatibilityController;
  @EJB
  private ProjectController projectController;

  private final static List<String> MYSQL_TYPES = Arrays.asList("INT", "TINYINT", "SMALLINT", "MEDIUMINT", "BIGINT",
      "FLOAT", "DOUBLE", "DECIMAL", "DATE", "DATETIME", "TIMESTAMP", "TIME", "YEAR", "CHAR", "BINARY", "BLOB", "TEXT",
      "TINYBLOB", "TINYTEXT", "MEDIUMBLOB", "MEDIUMTEXT", "LONGBLOB", "LONGTEXT");

  private final static String VARBINARY_DEFAULT = "VARBINARY(100)";
  private final static String CHAR_DEFAULT = "CHAR(100)";
  private static final String KAFKA_TOPIC_SUFFIX = "_onlinefs";

  public OnlineFeaturegroupController() {}

  protected OnlineFeaturegroupController(Settings settings) {
    this.settings = settings;
  }

      /**
       * Drops an online feature group, both the data-table in the database and the metadata record
       *
       * @param featuregroup featuregroup to delete
       * @param project
       * @param user
       * @throws SQLException
       * @throws FeaturestoreException
       */
  public void dropMySQLTable(Featuregroup featuregroup, Project project, Users user) throws SQLException,
    FeaturestoreException {
    //Drop data table
    String query = "DROP TABLE " + featuregroup.getName() + "_" + featuregroup.getVersion() + ";";
    onlineFeaturestoreController.executeUpdateJDBCQuery(query,
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()),
        project, user);
  }
  
  public void createMySQLTable(Featurestore featurestore, String tableName, List<FeatureGroupFeatureDTO> features,
                                             Project project, Users user)
      throws FeaturestoreException, SQLException{
    String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    String createStatement = buildCreateStatement(dbName, tableName, features);
    onlineFeaturestoreController.executeUpdateJDBCQuery(createStatement, dbName, project, user);
  }
  
  public void setupOnlineFeatureGroup(Featurestore featureStore, CachedFeaturegroupDTO cachedFeaturegroupDTO,
                                      List<FeatureGroupFeatureDTO> features, Project project, Users user)
      throws KafkaException, UserException, SQLException, SchemaException, ProjectException, FeaturestoreException,
      IOException, HopsSecurityException, ServiceException {
    setupOnlineFeatureGroup(featureStore, cachedFeaturegroupDTO.getName(), cachedFeaturegroupDTO.getVersion(),
      features, project, user);
  }
  
  public void setupOnlineFeatureGroup(Featurestore featureStore, Featuregroup featureGroup,
                                      List<FeatureGroupFeatureDTO> features, Project project, Users user)
      throws FeaturestoreException, SQLException, SchemaException, KafkaException, ProjectException, UserException,
      IOException, HopsSecurityException, ServiceException {
    setupOnlineFeatureGroup(featureStore, featureGroup.getName(), featureGroup.getVersion(), features, project, user);
  }
  
  public void setupOnlineFeatureGroup(Featurestore featureStore, String featureGroupName, Integer featureGroupVersion,
                                      List<FeatureGroupFeatureDTO> features, Project project, Users user)
      throws KafkaException, SchemaException, ProjectException, UserException, FeaturestoreException, SQLException,
      IOException, HopsSecurityException, ServiceException {
    // check if onlinefs user is part of project
    if (project.getProjectTeamCollection().stream().noneMatch(pt ->
      pt.getUser().getUsername().equals(OnlineFeaturestoreController.ONLINEFS_USERNAME))) {
      try {
        // wait for the future
        projectController.addOnlineFsUser(project).get();
      } catch (InterruptedException | ExecutionException e) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_GENERIC_ERROR,
          Level.SEVERE, "failed to add onlinefs user to project: " + project.getName(), e.getMessage(), e);
      }
    }
    
    String featureGroupEntityName = Utils.getFeatureStoreEntityName(featureGroupName, featureGroupVersion);

    createMySQLTable(featureStore, featureGroupEntityName, features, project, user);

    String avroSchema = avroSchemaConstructorController
        .constructSchema(featureGroupEntityName, Utils.getFeaturestoreName(project), features);
    schemasController.validateSchema(project, avroSchema);
    createOnlineKafkaTopic(project, featureGroupEntityName, avroSchema);
  }
  
  // For ingesting data in the online feature store, we setup a new topic for each feature group
  // The topic schema is also registered so it's available both for the hsfs library and for the collector
  private void createOnlineKafkaTopic(Project project, String featureGroupEntityName, String avroSchema)
      throws KafkaException, SchemaException, ProjectException, UserException {
    String topicName = onlineFeatureGroupTopicName(project.getId(), featureGroupEntityName);
    SubjectDTO topicSubject = subjectsController.registerNewSubject(project, topicName, avroSchema, false);
    subjectsCompatibilityController.setSubjectCompatibility(project, topicName, SchemaCompatibility.NONE);
    // TODO(Fabio): Make Kafka topics configurable
    TopicDTO topicDTO = new TopicDTO(topicName, 1, 1, topicSubject.getSubject(), topicSubject.getVersion());
    kafkaController.createTopic(project, topicDTO);
  }
  
  public String onlineFeatureGroupTopicName(Integer projectId, String featureGroupEntityName) {
    return projectId.toString() + "_" + featureGroupEntityName + KAFKA_TOPIC_SUFFIX;
  }
  
  public void deleteOnlineKafkaTopic(Project project, Featuregroup featureGroup)
    throws KafkaException, SchemaException {
    String topicName = onlineFeatureGroupTopicName(project.getId(), Utils.getFeaturegroupName(featureGroup));
    // user might have deleted topic manually
    if (kafkaController.projectTopicExists(project, topicName)) {
      kafkaController.removeTopicFromProject(project, topicName);
    }
    if (!subjectsController.getSubjectVersions(project, topicName).isEmpty()) {
      subjectsController.deleteSubject(project, topicName);
    }
  }
  
  public void alterOnlineFeatureGroupSchema(Featuregroup featureGroup, List<FeatureGroupFeatureDTO> newFeatures,
                                            Project project, Users user)
      throws FeaturestoreException, SchemaException, SQLException, KafkaException {
    String tableName = Utils.getFeatureStoreEntityName(featureGroup.getName(), featureGroup.getVersion());
    String topicName = onlineFeatureGroupTopicName(project.getId(), tableName);
    alterMySQLTableColumns(featureGroup.getFeaturestore(), tableName, newFeatures, project, user);
    // publish new version of avro schema
    String avroSchema = avroSchemaConstructorController.constructSchema(featureGroup.getName(),
      Utils.getFeaturestoreName(project), newFeatures);
    schemasController.validateSchema(project, avroSchema);
    SubjectDTO topicSubject = subjectsController.registerNewSubject(project, topicName, avroSchema, false);
    kafkaController.updateTopicSchemaVersion(project, topicName, topicSubject.getVersion());
  }
  
  public void disableOnlineFeatureGroup(Featuregroup featureGroup, Project project, Users user)
      throws FeaturestoreException, SQLException, SchemaException, KafkaException {
    dropMySQLTable(featureGroup, project, user);
    deleteOnlineKafkaTopic(project, featureGroup);
  }

  public String buildCreateStatement(String dbName, String tableName, List<FeatureGroupFeatureDTO> features) {
    StringBuilder createStatement = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    createStatement.append("`" + dbName + "`").append(".").append("`" + tableName + "`").append("(");

    // Add all features
    List<String> columns = new ArrayList<>();
    for (FeatureGroupFeatureDTO feature : features) {
      StringBuilder column = new StringBuilder();
      column.append("`" + feature.getName() + "`").append(" ").append(getOnlineType(feature));
      // at the moment only needed for online-enabling a previously offline-only feature group with appended features
      if (feature.getDefaultValue() != null) {
        column.append(" NOT NULL DEFAULT ");
        if (feature.getType().equalsIgnoreCase("string")) {
          column.append("'").append(feature.getDefaultValue()).append("'");
        } else {
          column.append(feature.getDefaultValue());
        }
      }
      columns.add(column.toString());
    }

    createStatement.append(StringUtils.join(columns, ", "));

    // add primary keys
    List<FeatureGroupFeatureDTO> pkFeatures = features.stream()
        .filter(FeatureGroupFeatureDTO::getPrimary)
        .collect(Collectors.toList());
    if (!pkFeatures.isEmpty()) {
      createStatement.append(", PRIMARY KEY (`");
      createStatement.append(
          StringUtils.join(pkFeatures.stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.toList()),
            "`,`"));
      createStatement.append("`)");
    }

    // Closing parenthesis
    createStatement.append(")");

    // READ_BACKUP improve reads as long as you don't take locks
    createStatement.append("ENGINE=ndbcluster ")
                   .append("COMMENT='NDB_TABLE=READ_BACKUP=1'");

    // Add tablespace if specified
    if (!Strings.isNullOrEmpty(settings.getOnlineFeatureStoreTableSpace())) {
      createStatement.append("/*!50100 TABLESPACE `")
                     .append(settings.getOnlineFeatureStoreTableSpace())
                     .append("` STORAGE DISK */");
    }

    return createStatement.toString();
  }

  public String buildAlterStatement(String tableName, String dbName, List<FeatureGroupFeatureDTO> featureDTOs) {
    StringBuilder alterTableStatement = new StringBuilder("ALTER TABLE `" + dbName + "`.`" + tableName + "` ");
    List<String> addColumn = new ArrayList<>();
    for (FeatureGroupFeatureDTO featureDTO : featureDTOs) {
      StringBuilder add =
        new StringBuilder("ADD COLUMN `" + featureDTO.getName() + "` " + getOnlineType(featureDTO));
      if (featureDTO.getDefaultValue() != null) {
        add.append(" NOT NULL DEFAULT ");
        if (featureDTO.getType().equalsIgnoreCase("string")) {
          add.append("'" + featureDTO.getDefaultValue() + "'");
        } else {
          add.append(featureDTO.getDefaultValue() + "");
        }
      } else {
        add.append(" DEFAULT NULL");
      }
      addColumn.add(add.toString());
    }
    alterTableStatement.append(StringUtils.join(addColumn, ", ") + ";");
    return alterTableStatement.toString();
  }

  public void alterMySQLTableColumns(Featurestore featurestore, String tableName,
                                     List<FeatureGroupFeatureDTO> featureDTOs, Project project, Users user)
      throws FeaturestoreException, SQLException {
    String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    onlineFeaturestoreController.executeUpdateJDBCQuery(buildAlterStatement(tableName, dbName, featureDTOs), dbName,
      project, user);
  }

  private String getOnlineType(FeatureGroupFeatureDTO featureGroupFeatureDTO) {
    if (!Strings.isNullOrEmpty(featureGroupFeatureDTO.getOnlineType())) {
      // TODO(Fabio): Check that it's a valid online type
      return featureGroupFeatureDTO.getOnlineType().toLowerCase();
    }

    if (MYSQL_TYPES.contains(featureGroupFeatureDTO.getType().toUpperCase())) {
      // Hive type and MySQL type match
      return featureGroupFeatureDTO.getType().toLowerCase();
    } else if (featureGroupFeatureDTO.getType().equalsIgnoreCase("boolean")) {
      return "tinyint";
    } else if (featureGroupFeatureDTO.getType().equalsIgnoreCase("string")) {
      return CHAR_DEFAULT;
    } else {
      return VARBINARY_DEFAULT;
    }
  }

  /**
   * Previews the contents of a online feature group (runs SELECT * LIMIT 20)
   *
   * @param featuregroup the online featuregroup to get the SQL schema of
   * @return the preview result
   * @throws FeaturestoreException
   * @throws SQLException
   */
  public FeaturegroupPreview getFeaturegroupPreview(Featuregroup featuregroup, Project project,
                                                    Users user, int limit)
      throws FeaturestoreException, SQLException {
    String tblName = featuregroup.getName() + "_" + featuregroup.getVersion();
    String query = "SELECT * FROM " + tblName + " LIMIT " + limit;
    String db = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    try {
      return onlineFeaturestoreController.executeReadJDBCQuery(query, db, project, user);
    } catch(Exception e) {
      return onlineFeaturestoreController.executeReadJDBCQuery(query, db, project, user);
    }
  }

  /**
   * Gets the SQL schema of an online feature group
   *
   * @param featuregroup the online featuregroup to get the SQL schema of
   * @return a String with the "SHOW CREATE TABLE" result
   */
  public String getFeaturegroupSchema(Featuregroup featuregroup) throws FeaturestoreException {
    return onlineFeaturestoreFacade.getMySQLSchema(
        Utils.getFeatureStoreEntityName(featuregroup.getName(), featuregroup.getVersion()),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
  }

  /**
   * Queries the metadata in MySQL-Cluster to get the schema information of an online feature group
   *
   * @param featuregroup the online featuregroup to get type information for
   * @return a list of Feature DTOs with the type information
   */
  public List<FeatureGroupFeatureDTO> getFeaturegroupFeatures(Featuregroup featuregroup) throws FeaturestoreException {
    return onlineFeaturestoreFacade.getMySQLFeatures(
        Utils.getFeatureStoreEntityName(featuregroup.getName(), featuregroup.getVersion()),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
  }

  public Long getFeaturegroupSize(Featuregroup featuregroup) {
    return onlineFeaturestoreFacade.getTblSize(
        Utils.getFeatureStoreEntityName(featuregroup.getName(), featuregroup.getVersion()),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
  }
}
