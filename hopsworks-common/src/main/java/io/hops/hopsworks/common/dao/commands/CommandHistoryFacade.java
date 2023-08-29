/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.commands;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.commands.CommandHistory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public abstract class CommandHistoryFacade<C extends CommandHistory> extends AbstractFacade<C> {
  @PersistenceContext(unitName = "kthfsPU")
  protected EntityManager em;
  
  protected CommandHistoryFacade(Class<C> entityClass) {
    super(entityClass);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  protected abstract String getTableName();
  
  public void persistAndFlush(C command) {
    em.persist(command);
    em.flush();
  }
  
  public void deleteOlderThan(Long interval) {
    String queryStr = "DELETE FROM " + getTableName() + " h ";
    queryStr += "WHERE h.executed < FUNCTION('DATEADD', 'MILLISECOND', :interval, CURRENT_TIMESTAMP)";
    Query q = em.createQuery(queryStr, entityClass);
    q.setParameter("interval", interval);
    q.executeUpdate();
  }
  
  public long countRetries(Long id) {
    String queryStr = "SELECT COUNT(*) FROM " + getTableName() + " h WHERE h.id = :id AND h.status = \"FAILED\"";
    Query q = em.createQuery(queryStr, Long.class);
    q.setParameter("id", id);
    return (Long)q.getSingleResult();
  }
}
