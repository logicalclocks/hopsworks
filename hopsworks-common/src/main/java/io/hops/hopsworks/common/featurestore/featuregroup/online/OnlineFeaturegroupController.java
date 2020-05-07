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

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeaturegroupPreview;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
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
   * @param featurestore the featurestore where the online featuregroup resides
   * @param user the user making the request
   * @throws SQLException
   * @throws FeaturestoreException
   */
  public void dropMySQLTable(
    OnlineFeaturegroup onlineFeaturegroup, Featurestore featurestore, Users user) throws SQLException,
    FeaturestoreException {
    //Drop data table
    String query = "DROP TABLE " + onlineFeaturegroup.getTableName() + ";";
    onlineFeaturestoreController.executeUpdateJDBCQuery(query, onlineFeaturegroup.getDbName(),
      featurestore.getProject(), user);
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
  
  /**
   * Creates a new table in the online feature store database
   *
   * @param featurestore the featurestore that the featuregroup belongs to
   * @param user the user making the request
   * @param featureStr DDL string
   * @param tableName name of the table to create
   * @return the created entity
   */
  public OnlineFeaturegroup createMySQLTable(Featurestore featurestore,
    Users user, String featureStr, String tableName) throws FeaturestoreException, SQLException {
    String db = onlineFeaturestoreController.getOnlineFeaturestoreDbName(featurestore.getProject());
    String query = "CREATE TABLE " + db + ".`" + tableName + "` " + featureStr;
    onlineFeaturestoreController.executeUpdateJDBCQuery(query, db, featurestore.getProject(), user);
    return persistOnlineFeaturegroupMetadata(db, tableName);
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
