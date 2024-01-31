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
import io.hops.hopsworks.common.featurestore.embedding.EmbeddingController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
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
import io.hops.hopsworks.multiregion.MultiRegionController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.kafka.schemas.SchemaCompatibility;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParserPos;

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
  @EJB
  private ConstructorController constructorController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private MultiRegionController multiRegionController;
  @EJB
  private EmbeddingController embeddingController;

  private final static List<String> SUPPORTED_MYSQL_TYPES = Arrays.asList("INT", "TINYINT",
      "SMALLINT", "BIGINT", "FLOAT", "DOUBLE", "DECIMAL", "DATE", "TIMESTAMP");

  private final static String VARBINARY_DEFAULT = "VARBINARY(100)";
  private final static String VARCHAR_DEFAULT = "VARCHAR(100)";

  public OnlineFeaturegroupController() {
  }

  protected OnlineFeaturegroupController(Settings settings, EmbeddingController embeddingController) {
    this.settings = settings;
    this.embeddingController = embeddingController;
  }

  public void setupOnlineFeatureGroup(Featurestore featureStore, Featuregroup featureGroup,
      List<FeatureGroupFeatureDTO> features, Project project, Users user)
      throws KafkaException, SchemaException, ProjectException, FeaturestoreException, IOException,
      HopsSecurityException, ServiceException {
    // online feature store is created only when creating the first feature group.
    createOnlineFeatureStore(project, featureStore);

    // check if onlinefs user is part of project
    checkOnlineFsUserExist(project);

    createFeatureGroupKafkaTopic(project, featureGroup, features);
    if (featureGroup.getEmbedding() != null) {
      embeddingController.createVectorDbIndex(project, featureGroup);
    } else {
      createMySQLTable(featureStore, Utils.getFeaturegroupName(featureGroup), features, project, user);
    }
  }

  void createOnlineFeatureStore(Project project, Featurestore featurestore)
      throws FeaturestoreException {
    // Create online feature store users for existing team members
    onlineFeaturestoreController.setupOnlineFeatureStore(project, featurestore);
  }

  void checkOnlineFsUserExist(Project project)
      throws ServiceException, HopsSecurityException, IOException, ProjectException {
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
  }

  void createMySQLTable(Featurestore featurestore, String tableName, List<FeatureGroupFeatureDTO> features,
      Project project, Users user)
      throws FeaturestoreException {
    String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    String createStatement = buildCreateStatement(dbName, tableName, features);
    onlineFeaturestoreFacade.executeUpdateJDBCQuery(createStatement, dbName, project, user, null);

    if (multiRegionController.isEnabled()) {
      onlineFeaturestoreFacade.executeUpdateJDBCQuery(createStatement, dbName, project, user,
          multiRegionController.getSecondaryRegionName());
    }
  }

  // For ingesting data in the online feature store, we set up a topic for project/feature group
  // The topic schema is registered for each feature group
  public void createFeatureGroupKafkaTopic(Project project, Featuregroup featureGroup,
                                           List<FeatureGroupFeatureDTO> features)
      throws KafkaException, SchemaException, FeaturestoreException {
    String avroSchema = avroSchemaConstructorController.constructSchema(featureGroup, features);
    schemasController.validateSchema(project, avroSchema);

    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    subjectsController.registerNewSubject(project, featureGroupEntityName, avroSchema, false);
    subjectsCompatibilityController.setSubjectCompatibility(project, featureGroupEntityName, SchemaCompatibility.NONE);

    String topicName = Utils.getFeatureGroupTopicName(featureGroup);

    try {
      TopicDTO topicDTO = new TopicDTO(topicName,
          settings.getKafkaDefaultNumReplicas(),
          settings.getOnlineFsThreadNumber());
      kafkaController.createTopic(project, topicDTO);
    } catch (KafkaException e) {
      // if topic already exists, no need to create it again.
      if (e.getErrorCode() != RESTCodes.KafkaErrorCode.TOPIC_ALREADY_EXISTS) {
        throw e;
      }
    }
  }

  public void alterOnlineFeatureGroupSchema(Featuregroup featureGroup, List<FeatureGroupFeatureDTO> newFeatures,
                                            List<FeatureGroupFeatureDTO> fullNewSchema,
                                            Project project, Users user)
      throws FeaturestoreException, SchemaException, SQLException, KafkaException {
    String tableName = Utils.getFeaturegroupName(featureGroup);
    alterMySQLTableColumns(featureGroup.getFeaturestore(), tableName, newFeatures, project, user);
    alterFeatureGroupSchema(featureGroup, fullNewSchema, project);
  }

  public void alterFeatureGroupSchema(Featuregroup featureGroup, List<FeatureGroupFeatureDTO> fullNewSchema,
                                      Project project)
      throws FeaturestoreException, SchemaException, KafkaException {
    // publish new version of avro schema
    String avroSchema = avroSchemaConstructorController.constructSchema(featureGroup, fullNewSchema);
    schemasController.validateSchema(project, avroSchema);
    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    subjectsController.registerNewSubject(project, featureGroupEntityName, avroSchema, false);
  }

  public void disableOnlineFeatureGroup(Featuregroup featureGroup, Project project, Users user)
      throws FeaturestoreException, SchemaException, KafkaException {
    if (featureGroup.getEmbedding() != null) {
      embeddingController.dropEmbedding(project, featureGroup);
    } else {
      dropMySQLTable(featureGroup, project, user);
    }
    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    if (!subjectsController.getSubjectVersions(project, featureGroupEntityName).isEmpty()) {
      subjectsController.deleteSubject(project, featureGroupEntityName);
    }
    // HOPSWORKS-3252 - we keep kafka topics in order to avoid consumers getting blocked
    // deleteFeatureGroupKafkaTopic(project, topicName);
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
  public void dropMySQLTable(Featuregroup featuregroup, Project project, Users user) throws FeaturestoreException {
    //Drop data table
    String query = "DROP TABLE " + featuregroup.getName() + "_" + featuregroup.getVersion() + ";";
    onlineFeaturestoreFacade.executeUpdateJDBCQuery(query,
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()),
        project, user, null);

    if (multiRegionController.isEnabled()) {
      onlineFeaturestoreFacade.executeUpdateJDBCQuery(query,
          onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()),
          project, user, multiRegionController.getSecondaryRegionName());
    }
  }

  public void deleteFeatureGroupKafkaTopic(Project project, Featuregroup featureGroup)
      throws KafkaException, SchemaException {
    String topicName = Utils.getFeatureGroupTopicName(featureGroup);
    String featureGroupEntityName = Utils.getFeaturegroupName(featureGroup);
    // user might have deleted topic manually
    if (topicName.equals(featureGroup.getTopicName())) {
      // delete topic only if it is unique to fg
      kafkaController.removeTopicFromProject(project, topicName);
    }
    if (!subjectsController.getSubjectVersions(project, featureGroupEntityName).isEmpty()) {
      subjectsController.deleteSubject(project, featureGroupEntityName);
    }
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
      if (featureDTO.getDefaultValue() == null) {
        add.append(" DEFAULT NULL");
      }
      addColumn.add(add.toString());
    }
    addColumn.add("ALGORITHM=INPLACE");
    alterTableStatement.append(StringUtils.join(addColumn, ", ") + ";");
    return alterTableStatement.toString();
  }

  public void alterMySQLTableColumns(Featurestore featurestore, String tableName,
                                     List<FeatureGroupFeatureDTO> featureDTOs, Project project, Users user)
      throws FeaturestoreException {
    String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    onlineFeaturestoreFacade.executeUpdateJDBCQuery(
        buildAlterStatement(tableName, dbName, featureDTOs), dbName, project, user, null);

    if (multiRegionController.isEnabled()) {
      onlineFeaturestoreFacade.executeUpdateJDBCQuery(
          buildAlterStatement(tableName, dbName, featureDTOs), dbName, project, user,
          multiRegionController.getSecondaryRegionName());
    }
  }

  public String getOnlineType(FeatureGroupFeatureDTO featureGroupFeatureDTO) {
    if (!Strings.isNullOrEmpty(featureGroupFeatureDTO.getOnlineType())) {
      return featureGroupFeatureDTO.getOnlineType().toLowerCase();
    }

    for (String mysqlType : SUPPORTED_MYSQL_TYPES) {
      // User startsWith to handle offline types like decimal(X, Y) where X and Y depend on the context
      // Same for CHAR(X) where X is the length of the char. We are not particularly interested in the
      // type configuration, but rather if we can use the same Hive type on MySQL or if we need
      // to handle it separately.
      if (featureGroupFeatureDTO.getType().toUpperCase().startsWith(mysqlType)) {
        return featureGroupFeatureDTO.getType();
      }
    }

    if (featureGroupFeatureDTO.getType().equalsIgnoreCase("boolean")) {
      return "tinyint";
    } else if (featureGroupFeatureDTO.getType().equalsIgnoreCase("string")) {
      return VARCHAR_DEFAULT;
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
  public FeaturegroupPreview getFeaturegroupPreview(Featuregroup featuregroup, Project project, Users user, int limit)
      throws FeaturestoreException {
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

    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList,
        new SqlIdentifier("`" + tbl + "`", SqlParserPos.ZERO),
        null, null, null, null, null, null,
        SqlLiteral.createExactNumeric(String.valueOf(limit), SqlParserPos.ZERO), null);
    String db = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject());
    try {
      return onlineFeaturestoreFacade.executeReadJDBCQuery(
          select.toSqlString(new MysqlSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql(), db, project, user);
    } catch(Exception e) {
      return onlineFeaturestoreFacade.executeReadJDBCQuery(
          select.toSqlString(new MysqlSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql(), db, project, user);
    }
  }

  /**
   * Queries the metadata in MySQL-Cluster to get the schema information of an online feature group
   *
   * @param featuregroup the online featuregroup to get type information for
   * @param featureGroupFeatureDTOS list of feature dtos of the online featuregroup
   * @return a list of Feature DTOs with the type information
   */
  public List<FeatureGroupFeatureDTO> getFeaturegroupFeatures(Featuregroup featuregroup,
    List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS) throws FeaturestoreException {
    List<FeatureGroupFeatureDTO> onlineFeatureGroupFeatureDTOS = onlineFeaturestoreFacade.getMySQLFeatures(
        Utils.getFeatureStoreEntityName(featuregroup.getName(), featuregroup.getVersion()),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
    for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOS) {
      for (FeatureGroupFeatureDTO onlineFeatureGroupFeatureDTO : onlineFeatureGroupFeatureDTOS) {
        if(featureGroupFeatureDTO.getName().equalsIgnoreCase(onlineFeatureGroupFeatureDTO.getName())){
          featureGroupFeatureDTO.setOnlineType(onlineFeatureGroupFeatureDTO.getType());
        }
      }
    }
    return featureGroupFeatureDTOS;
  }
}
