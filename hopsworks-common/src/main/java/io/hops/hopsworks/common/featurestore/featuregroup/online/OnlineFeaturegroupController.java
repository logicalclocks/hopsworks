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
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  private final static List<String> MYSQL_TYPES = Arrays.asList("INT", "TINYINT", "SMALLINT", "MEDIUMINT", "BIGINT",
      "FLOAT", "DOUBLE", "DECIMAL", "DATE", "DATETIME", "TIMESTAMP", "TIME", "YEAR", "CHAR", "BLOB", "TEXT",
      "TINYBLOB", "TINYTEXT", "MEDIUMBLOB", "MEDIUMTEXT", "LONGBLOB", "LONGTEXT");

  private final static String VARBINARY_DEFAULT = "VARBINARY(100)";
  private final static String CHAR_DEFAULT = "CHAR(100)";

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

  public String buildCreateStatement(String dbName, String tableName, List<FeatureGroupFeatureDTO> features) {
    StringBuilder createStatement = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    createStatement.append("`" + dbName + "`").append(".").append("`" + tableName + "`").append("(");

    // Add all features
    for (FeatureGroupFeatureDTO feature : features) {
      createStatement.append("`" + feature.getName() + "`").append(" ").append(getOnlineType(feature));
      // at the moment only needed for online-enabling a previously offline-only feature group with appended features
      if (feature.getDefaultValue() != null) {
        createStatement.append(" NOT NULL DEFAULT ");
        if (feature.getType().equalsIgnoreCase("string")) {
          createStatement.append("'").append(feature.getDefaultValue()).append("'");
        } else {
          createStatement.append(feature.getDefaultValue());
        }
      }
    }

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
        getTblName(featuregroup),
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
        getTblName(featuregroup),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
  }

  public Long getFeaturegroupSize(Featuregroup featuregroup) {
    return onlineFeaturestoreFacade.getTblSize(
        getTblName(featuregroup),
        onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
  }

  private String getTblName(Featuregroup featuregroup) {
    return featuregroup.getName() + "_" + featuregroup.getVersion();
  }
}
