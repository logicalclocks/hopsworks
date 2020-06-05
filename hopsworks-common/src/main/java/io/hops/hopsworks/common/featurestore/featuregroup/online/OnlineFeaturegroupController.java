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
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online.OnlineFeaturegroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the online_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OnlineFeaturegroupController {
  @EJB
  private OnlineFeaturegroupFacade onlineFeaturegroupFacade;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private Settings settings;
  
  /**
   * Persists metadata of a new online feature group in the online_feature_group table
   *
   * @param dbName name of the MySQL database where the online feature group data is stored
   * @param tableName name of the MySQL table where the online feature group data is stored
   * @return Entity of the created online feature group
   */
  private OnlineFeaturegroup persistOnlineFeaturegroupMetadata(String dbName, String tableName) {
    OnlineFeaturegroup onlineFeaturegroup = new OnlineFeaturegroup();
    onlineFeaturegroup.setDbName(dbName);
    onlineFeaturegroup.setTableName(tableName);
    onlineFeaturegroupFacade.persist(onlineFeaturegroup);
    return onlineFeaturegroup;
  }
  
  /**
   * Drops an online feature group, both the data-table in the database and the metadata record
   *
   * @param onlineFeaturegroup the online featuregroup to delete
   * @param project
   * @param user the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   */
  public void dropMySQLTable(OnlineFeaturegroup onlineFeaturegroup, Project project, Users user) throws SQLException,
    FeaturestoreException {
    //Drop data table
    String query = "DROP TABLE " + onlineFeaturegroup.getTableName() + ";";
    onlineFeaturestoreController.executeUpdateJDBCQuery(query, onlineFeaturegroup.getDbName(), project, user);
    //Drop metadata
    removeOnlineFeaturegroupMetadata(onlineFeaturegroup);
  }
  
  /**
   * Removes the metadata of an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup
   * @return the deleted entity
   */
  public OnlineFeaturegroup removeOnlineFeaturegroupMetadata(OnlineFeaturegroup onlineFeaturegroup){
    onlineFeaturegroupFacade.remove(onlineFeaturegroup);
    return onlineFeaturegroup;
  }

  public OnlineFeaturegroup createMySQLTable(Featurestore featurestore, String tableName, List<FeatureDTO> features,
                                             Project project, Users user)
      throws FeaturestoreException, SQLException{
    String dbName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    String createStatement = buildCreateStatement(dbName, tableName, features);
    onlineFeaturestoreController.executeUpdateJDBCQuery(createStatement, dbName, project, user);
    return persistOnlineFeaturegroupMetadata(dbName, tableName);
  }

  private String buildCreateStatement(String dbName, String tableName, List<FeatureDTO> features) {
    StringBuilder createStatement = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    createStatement.append(dbName).append(".").append(tableName).append("(");

    // Add all features
    for (FeatureDTO feature : features) {
      createStatement.append(feature.getName()).append(" ").append(feature.getOnlineType()).append(",");
    }

    // add primary keys
    List<FeatureDTO> pkFeatures = features.stream().filter(FeatureDTO::getPrimary).collect(Collectors.toList());
    if (!pkFeatures.isEmpty()) {
      createStatement.append("PRIMARY KEY (");
      createStatement.append(
          StringUtils.join(pkFeatures.stream().map(FeatureDTO::getName).collect(Collectors.toList()), ","));
      createStatement.append(")");
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
  
  /**
   * Converts a Online Featuregroup entity into a DTO representation
   *
   * @param onlineFeaturegroup the online featuregroup to convert
   * @return a DTO representation of the online feature group
   */
  public OnlineFeaturegroupDTO convertOnlineFeaturegroupToDTO(OnlineFeaturegroup onlineFeaturegroup){
    OnlineFeaturegroupDTO onlineFeaturegroupDTO = new OnlineFeaturegroupDTO(onlineFeaturegroup);
    onlineFeaturegroupDTO.setTableType(
      onlineFeaturestoreController.getOnlineFeaturegroupTableType(onlineFeaturegroupDTO));
    onlineFeaturegroupDTO.setSize(
      onlineFeaturestoreController.getTblSize(onlineFeaturegroupDTO));
    onlineFeaturegroupDTO.setTableRows(
      onlineFeaturestoreController.getOnlineFeaturegroupTableRows(onlineFeaturegroupDTO));
    return onlineFeaturegroupDTO;
  }
  
  /**
   * Queries the metadata in MySQL-Cluster to get the schema information of an online feature group
   *
   * @param onlineFeaturegroup the online featuregroup to get type information for
   * @return a list of Feature DTOs with the type information
   */
  public List<FeatureDTO> getOnlineFeaturegroupFeatures(OnlineFeaturegroup onlineFeaturegroup) {
    return onlineFeaturestoreController.getOnlineFeaturegroupFeatures(onlineFeaturegroup);
  }

  /**
   * Previews the contents of a online feature group (runs SELECT * LIMIT 20)
   *
   * @return the preview result
   * @throws FeaturestoreException
   * @throws SQLException
   */
  public FeaturegroupPreview getOnlineFeaturegroupPreview(Featuregroup featuregroup, Project project,
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
  
}
