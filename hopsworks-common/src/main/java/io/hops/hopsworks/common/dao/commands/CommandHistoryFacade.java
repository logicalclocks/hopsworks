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

import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.commands.CommandHistory;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

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
  
  public void deleteOlderThan(Long intervalSeconds) throws CommandException {
  
    Connection connection = em.unwrap(Connection.class);
  
    String sql = "DELETE FROM hopsworks.command_search_fs_history h" +
      " WHERE h.executed < DATE_ADD(CURRENT_TIMESTAMP, INTERVAL ? SECOND)";
    int deleted = 0;
    do {
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setLong(1, (-1) * intervalSeconds);
        preparedStatement.setMaxRows(10000);
        deleted = preparedStatement.executeUpdate();
      } catch (SQLException e) {
        String msg = "error on the command history cleanup query";
        throw new CommandException(RESTCodes.CommandErrorCode.DB_QUERY_ERROR, Level.WARNING, msg, msg, e);
      }
    } while(deleted > 0);
  }
  
  public long countRetries(Long id) {
    String queryStr = "SELECT COUNT(h.id) FROM " + getTableName() + " h WHERE h.id = :id AND h.status = :status";
    Query q = em.createQuery(queryStr, entityClass);
    q.setParameter("id", id);
    q.setParameter("status", CommandStatus.FAILED);
    return (Long)q.getSingleResult();
  }
}
