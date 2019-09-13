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

package io.hops.hopsworks.common.dao.featurestore.online_featurestore;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * A facade for the online feature store databases (separate from the Hopsworks databases.
 * This interface is supposed to be used for any DDL queries on online feature store databases. DML and analytic
 * queries are done through JDBC and client-libraries.
 */
@Stateless
public class OnlineFeaturestoreFacade {
  private static final Logger LOGGER = Logger.getLogger(OnlineFeaturestoreFacade.class.getName());
  @PersistenceContext(unitName = "featurestorePU")
  private EntityManager em;
  
  /**
   * Gets the size of an online featurestore database. I.e the size of a MySQL-cluster database.
   *
   * @param dbName the name of the database
   * @return the size in MB
   */
  public Double getDbSize(String dbName) {
    try {
      return ((BigDecimal) em.createNativeQuery("SELECT " +
        "ROUND(SUM(`tables`.`data_length` + `index_length`) / 1024 / 1024, 1) AS 'size_mb' " +
        "FROM information_schema.`tables` " +
        "WHERE `tables`.`table_schema`=? GROUP BY `tables`.`table_schema`")
        .setParameter(1, dbName)
        .getSingleResult()).doubleValue();
    } catch (NoResultException e) {
      return null;
    }
  }
}
