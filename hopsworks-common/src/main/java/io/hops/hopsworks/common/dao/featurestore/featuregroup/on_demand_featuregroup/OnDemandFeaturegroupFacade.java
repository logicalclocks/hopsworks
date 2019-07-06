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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the on_demand_feature_group table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class OnDemandFeaturegroupFacade extends AbstractFacade<OnDemandFeaturegroup> {
  private static final Logger LOGGER = Logger.getLogger(
    OnDemandFeaturegroupFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public OnDemandFeaturegroupFacade() {
    super(OnDemandFeaturegroup.class);
  }

  /**
   * A transaction to persist an on-demand featuregroup for the featurestore in the database
   *
   * @param onDemandFeaturegroup the on-demand featuregroup to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(OnDemandFeaturegroup onDemandFeaturegroup) {
    try {
      em.persist(onDemandFeaturegroup);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new on demand feature group", cve);
      throw cve;
    }
  }
  
  /**
   * Updates an existing On Demand Feature Group
   *
   * @param onDemandFeaturegroup the entity to update
   * @param featuregroupName the new name
   * @param description the new description
   * @param jdbcConnector the new JDBC connector
   * @param sqlQuery the new SQL query
   * @return the updated entity
   */
  public OnDemandFeaturegroup updateMetadata(OnDemandFeaturegroup onDemandFeaturegroup,
    String featuregroupName, String description, FeaturestoreJdbcConnector jdbcConnector,
    String sqlQuery) {
    onDemandFeaturegroup.setName(featuregroupName);
    onDemandFeaturegroup.setDescription(description);
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(jdbcConnector);
    onDemandFeaturegroup.setQuery(sqlQuery);
    em.merge(onDemandFeaturegroup);
    return onDemandFeaturegroup;
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

}
