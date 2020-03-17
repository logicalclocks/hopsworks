/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.featurestore.tag;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagType;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Set;

@Stateless
public class FeatureStoreTagFacade extends AbstractFacade<FeatureStoreTag> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public FeatureStoreTagFacade() {
    super(FeatureStoreTag.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public FeatureStoreTag findByName(String name) {
    TypedQuery<FeatureStoreTag> q = em.createNamedQuery("FeatureStoreTag.findByName", FeatureStoreTag.class);
    q.setParameter("name", name);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
    }
    return null;
  }
  
  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort) {
    String queryStr = buildQuery("SELECT f FROM FeatureStoreTag f ", filter, sort, null);
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT f.id) FROM FeatureStoreTag f ", filter, null,
      null);
    Query query = em.createQuery(queryStr, FeatureStoreTag.class);
    Query queryCount = em.createQuery(queryCountStr, FeatureStoreTag.class);
    setFilter(filter, query);
    setFilter(filter, queryCount);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case ID:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      case NAME:
      case NAME_LIKE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case TYPE:
        q.setParameter(filterBy.getField(), getEnumValue(filterBy.getField(), filterBy.getParam(), TagType.class));
        break;
    }
  }
  
  public enum Sorts {
    ID("ID", " f.id ", "ASC"),
    NAME("NAME", " LOWER(f.name) ", "ASC"),
    TYPE("TYPE", " LOWER(f.type) ", "ASC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    Sorts(String value, String sql, String defaultParam) {
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
    
    @Override
    public String toString() {
      return value;
    }
  }
  
  public enum Filters {
    ID("ID", " f.id = :id", "id" , ""),
    NAME("NAME", " f.name = :name", "name" , ""),
    NAME_LIKE("NAME_LIKE", " UPPER(f.name) LIKE CONCAT(:name_like, '%')", "name_like" , ""),
    TYPE("TYPE", " f.type = :type", "type", "");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    Filters(String value, String sql, String field, String defaultParam) {
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
