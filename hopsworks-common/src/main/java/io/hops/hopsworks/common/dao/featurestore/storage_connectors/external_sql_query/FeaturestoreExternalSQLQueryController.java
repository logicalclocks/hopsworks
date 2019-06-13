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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.external_sql_query;

import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnector;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Class controlling the interaction with the feature_store_external_sql_query table and required business logic
 */
@Stateless
public class FeaturestoreExternalSQLQueryController {
  @EJB
  private FeaturestoreExternalSQLQueryFacade featurestoreExternalSQLQueryFacade;
  
  /**
   * Persists a JDBC connection for the feature store
   *
   * @param name name of the connection
   * @param description description of the connection
   * @param featurestore the feature store
   * @param arguments name of missing arguments for the JDBC string (e.g passwords and similar that should not be
   *                  stored plaintext in the db)
   * @param connectionString the JDBC connection string (arguments will be appended at runtime)
   * @return a DTO representing the created entity
   */
  
  /**
   * Persists an external SQL query for the feature store
   *
   * @param query the SQL query
   * @param description description of the query
   * @param name name of the query
   * @param featurestoreJdbcConnector the JDBC connector to apply the query
   * @return
   */
  public FeaturestoreExternalSQLQueryDTO createFeaturestoreExternalSqlQuery(String query, String description,
    String name, FeaturestoreJdbcConnector featurestoreJdbcConnector){
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery = new FeaturestoreExternalSQLQuery();
    featurestoreExternalSQLQuery.setDescription(description);
    featurestoreExternalSQLQuery.setName(name);
    featurestoreExternalSQLQuery.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    featurestoreExternalSQLQuery.setQuery(query);
    featurestoreExternalSQLQueryFacade.persist(featurestoreExternalSQLQuery);
    return new FeaturestoreExternalSQLQueryDTO(featurestoreExternalSQLQuery);
  }
  
  /**
  /**
   * Removes an external SQL query from the database with a particular id
   *
   * @param externalSqlQueryId id of the external sql query
   * @return DTO of the deleted entity
   */
  public FeaturestoreExternalSQLQueryDTO removeFeaturestoreJdbc(Integer externalSqlQueryId){
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery =
      featurestoreExternalSQLQueryFacade.find(externalSqlQueryId);
    FeaturestoreExternalSQLQueryDTO featurestoreExternalSQLQueryDTO =
      new FeaturestoreExternalSQLQueryDTO(featurestoreExternalSQLQuery);
    featurestoreExternalSQLQueryFacade.remove(featurestoreExternalSQLQuery);
    return featurestoreExternalSQLQueryDTO;
  }

}
