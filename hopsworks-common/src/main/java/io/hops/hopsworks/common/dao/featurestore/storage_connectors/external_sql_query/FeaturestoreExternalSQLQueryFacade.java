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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.external_sql_query;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A facade for the feature_store_external_sql_query table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class FeaturestoreExternalSQLQueryFacade extends AbstractFacade<FeaturestoreExternalSQLQuery> {
  private static final Logger LOGGER = Logger.getLogger(
    FeaturestoreExternalSQLQueryFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreExternalSQLQueryFacade() {
    super(FeaturestoreExternalSQLQuery.class);
  }

  /**
   * A transaction to persist an external SQL query for the featurestore in the database
   *
   * @param featurestoreExternalSQLQuery the featurestore JDBC connection to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery) {
    try {
      em.persist(featurestoreExternalSQLQuery);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new external SQL query", cve);
      throw cve;
    }
  }
  
  /**
   * Updates an existing Featurestore External SQL Query
   *
   * @param featurestoreExternalSQLQuery the entity to update
   * @param featuregroupName the new name
   * @param description the new description
   * @param jdbcConnector the new JDBC connector
   * @param sqlQuery the new SQL query
   * @return the updated entity
   */
  public FeaturestoreExternalSQLQuery updateMetadata(FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery,
    String featuregroupName, String description, FeaturestoreJdbcConnector jdbcConnector,
    String sqlQuery) {
    featurestoreExternalSQLQuery.setName(featuregroupName);
    featurestoreExternalSQLQuery.setDescription(description);
    featurestoreExternalSQLQuery.setFeaturestoreJdbcConnector(jdbcConnector);
    featurestoreExternalSQLQuery.setQuery(sqlQuery);
    em.merge(featurestoreExternalSQLQuery);
    return featurestoreExternalSQLQuery;
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
