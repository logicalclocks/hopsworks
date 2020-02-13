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

package io.hops.hopsworks.common.featurestore.statistics.columns;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.columns.StatisticColumn;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class StatisticColumnFacade extends AbstractFacade<StatisticColumn> {
  private static final Logger LOGGER = Logger.getLogger(StatisticColumnFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public StatisticColumnFacade() {
    super(StatisticColumn.class);
  }
  
  /**
   * A transaction to persist a statistic column in the database
   *
   * @param statisticColumn the statistic column to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(StatisticColumn statisticColumn) {
    try {
      em.persist(statisticColumn);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new StatisticColumn", cve);
      throw cve;
    }
  }
  
  /**
   * Gets the statistic columns of a ceratin feature group
   *
   * @param featuregroup
   * @return list of statistic columns
   */
  public List<StatisticColumn> findByFeaturegroup(Featuregroup featuregroup) {
    return em.createNamedQuery("StatisticColumn.findByFeaturegroup", StatisticColumn.class)
            .setParameter("feature_group", featuregroup)
            .getResultList();
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
