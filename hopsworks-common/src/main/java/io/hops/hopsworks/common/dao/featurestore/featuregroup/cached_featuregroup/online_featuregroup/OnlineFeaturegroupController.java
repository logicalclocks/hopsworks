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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.online_featuregroup;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.SQLException;

/**
 * Class controlling the interaction with the online_feature_group table and required business logic
 */
@Stateless
public class OnlineFeaturegroupController {
  @EJB
  private OnlineFeaturegroupFacade onlineFeaturegroupFacade;
  
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
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void dropMySQLTable(
    FeaturegroupDTO featuregroupDTO, Featurestore featurestore, Users user) throws SQLException,
    FeaturestoreException, HopsSecurityException {
    //TODO
  }
  
  public OnlineFeaturegroup createMySQLTable(CachedFeaturegroupDTO cachedFeaturegroupDTO, Featurestore featurestore,
    Users user) {
    //TODO
    return null;
  }
  
}
