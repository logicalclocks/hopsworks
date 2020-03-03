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
import io.hops.hopsworks.common.featurestore.featuregroup.cached.RowValueQueryResult;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online.OnlineFeaturegroup;
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
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
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<FeatureDTO> getOnlineFeaturegroupFeatures(OnlineFeaturegroup onlineFeaturegroup) {
    return onlineFeaturestoreController.getOnlineFeaturegroupFeatures(onlineFeaturegroup);
  }
  
  /**
   * Gets the SQL schema of an online feature group
   *
   * @param onlineFeaturegroupDTO the online featuregroup to get the SQL schema of
   * @return a String with the "SHOW CREATE TABLE" result
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String getOnlineFeaturegroupSchema(OnlineFeaturegroupDTO onlineFeaturegroupDTO) {
    return onlineFeaturestoreController.getOnlineFeaturegroupSchema(onlineFeaturegroupDTO);
  }
  
  /**
   * Previews the contents of a online feature group (runs SELECT * LIMIT 20)
   *
   * @param onlineFeaturegroupDTO the online featuregroup to get the SQL schema of
   * @return the preview result
   * @throws FeaturestoreException
   * @throws SQLException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<RowValueQueryResult> getOnlineFeaturegroupPreview(OnlineFeaturegroupDTO onlineFeaturegroupDTO,
                                                                Users user, Featurestore featurestore)
      throws FeaturestoreException, SQLException {
    return onlineFeaturestoreController.getOnlineFeaturegroupPreview(onlineFeaturegroupDTO, user, featurestore);
  }
  
}
