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
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;
import io.hops.hopsworks.persistence.entity.commands.Command;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class CommandFacade<C extends Command> extends AbstractFacade<C> {
  public static final String STATUS_FIELD = "status";
  public static final String PROJECT_ID_FIELD = "projectId";
  
  @PersistenceContext(unitName = "kthfsPU")
  protected EntityManager em;
  
  protected CommandFacade(Class<C> entityClass) {
    super(entityClass);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  protected abstract String getTableName();
  
  public C findById(Long id) {
    String queryStr = "SELECT c FROM " + getTableName() + " c WHERE c.id = :id";
    TypedQuery<C> query = em.createQuery(queryStr, entityClass);
    query.setParameter("id", id);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<C> findByQuery(QueryParam queryParam) throws CommandException {
    if(queryParam == null) {
      throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO, "query param is null");
    }
    String queryStrPrefix = "SELECT c FROM " + getTableName() + " c ";
    String queryStr = buildQuery(queryStrPrefix, queryParam.getFilters(), queryParam.getSorts(), "");
    Query q = em.createQuery(queryStr, entityClass);
    setParams(q, queryParam.getFilters());
    if(queryParam.getLimit() != null) {
      q.setMaxResults(queryParam.getLimit());
    }
    return q.getResultList();
  }
  
  public List<C> updateByQuery(QueryParam queryParam, Consumer<C> update) throws CommandException {
    List<C> result = findByQuery(queryParam);
    result.forEach(c -> {
      update.accept(c);
      update(c);
    });
    return result;
  }
  
  public void removeById(Long commandId) {
    remove(findById(commandId));
  }
  
  public void persistAndFlush(C command) {
    em.persist(command);
    em.flush();
  }
  
  protected void setParams(Query q, Set<AbstractFacade.FilterBy> filters) throws CommandException {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    for (AbstractFacade.FilterBy filter : filters) {
      if(filter.getParam() != null) {
        setParam(q, filter);
      }
    }
  }
  
  protected void setParam(Query q, AbstractFacade.FilterBy filterBy) throws CommandException {
    try {
      if(filterBy.getField().equals(STATUS_FIELD)) {
        q.setParameter(filterBy.getField(), CommandStatus.valueOf(filterBy.getParam()));
      } else if(filterBy.getField().equals(PROJECT_ID_FIELD)){
        q.setParameter(filterBy.getField(), Integer.parseInt(filterBy.getParam()));
      } else {
        String msg = "invalid filter:" + filterBy.toString();
        throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO, msg);
      }
    } catch (IllegalArgumentException e) {
      String msg = "invalid filter:" + filterBy.toString();
      throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO,
        msg, msg, e);
    }
  }
  
  public enum Filters implements CommandFilter {
    STATUS_EQ(STATUS_FIELD, "c.status = :status ", "NEW"),
    STATUS_NEQ(STATUS_FIELD,"c.status != :status ",  "NEW"),
    PROJECT_ID_EQ(PROJECT_ID_FIELD, "c." + PROJECT_ID_FIELD + " = :" + PROJECT_ID_FIELD, "0");
    
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    Filters(String field, String sql, String defaultParam) {
      this.field = field;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
  
    @Override
    public String getValue() {
      return this.name();
    }
  
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return this.name();
    }
  }
}
