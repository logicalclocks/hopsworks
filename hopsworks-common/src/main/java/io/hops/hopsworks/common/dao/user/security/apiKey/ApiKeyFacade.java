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
package io.hops.hopsworks.common.dao.user.security.apiKey;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Stateless
public class ApiKeyFacade extends AbstractFacade<ApiKey> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public ApiKeyFacade() {
    super(ApiKey.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  
  public ApiKey findByPrefix(String prefix) {
    TypedQuery<ApiKey> query = em.createNamedQuery("ApiKey.findByPrefix",
      ApiKey.class).setParameter("prefix", prefix);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public ApiKey findByUserAndName(Users user, String name) {
    TypedQuery<ApiKey> query = em.createNamedQuery("ApiKey.findByUserAndName",
      ApiKey.class).setParameter("user", user).setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<ApiKey> findByUser(Users user) {
    TypedQuery<ApiKey> query = em.createNamedQuery("ApiKey.findByUser",
      ApiKey.class).setParameter("user", user);
    return query.getResultList();
  }
  
  public CollectionInfo findByUser(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort, Users user) {
    String queryStr = buildQuery("SELECT a FROM ApiKey a ", filter, sort, "a.user = :user ");
    String queryCountStr = buildQuery("SELECT COUNT(a.id) FROM ApiKey a ", filter, sort, "a.user = :user ");
    Query query = em.createQuery(queryStr, ApiKey.class).setParameter("user", user);
    Query queryCount = em.createQuery(queryCountStr, ApiKey.class).setParameter("user", user);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  private CollectionInfo findAll(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter, Query query, Query queryCount) {
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filters, Query q) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    for (FilterBy filter : filters) {
      setFilterQuery(filter, q);
    }
  }
  
  private void setFilterQuery(FilterBy filter, Query q) {
    switch (Filters.valueOf(filter.getValue())) {
      case NAME:
        q.setParameter(filter.getField(), filter.getParam());
        break;
      case CREATED:
      case MODIFIED:
      case CREATED_GT:
      case CREATED_LT:
      case MODIFIED_GT:
      case MODIFIED_LT:
        Date date = getDate(filter.getField(), filter.getParam());
        q.setParameter(filter.getField(), date);
        break;
      default:
        break;
      
    }
  }
  
  public enum Sorts {
    NAME("NAME", "a.name ", "ASC"),
    MODIFIED("MODIFIED", "a.modified ", "ASC"),
    CREATED("CREATED", "a.created ", "ASC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getJoin() {
      return null;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    NAME("NAME", "a.name IN :name ", "name", " "),
    MODIFIED("MODIFIED", "a.modified = :modified ", "modified", "1970-01-01T00:00:00.000"),
    MODIFIED_LT("MODIFIED_LT", "a.modified < :modified_lt ", "modified_lt", "1970-01-01T00:00:00.000"),
    MODIFIED_GT("MODIFIED_GT", "a.modified > :modified_gt ", "modified_gt", "1970-01-01T00:00:00.000"),
    CREATED("CREATED", "a.created = :created ", "created", "1970-01-01T00:00:00.000"),
    CREATED_LT("CREATED_LT", "a.created < :created_lt ", "created_lt", "1970-01-01T00:00:00.000"),
    CREATED_GT("CREATED_GT", "a.created > :created_gt ", "created_gt", "1970-01-01T00:00:00.000");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
}
