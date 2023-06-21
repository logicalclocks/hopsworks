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
package io.hops.hopsworks.common.dao.remote.group;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

@Stateless
public class RemoteGroupProjectMappingFacade extends AbstractFacade<RemoteGroupProjectMapping> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  ProjectFacade projectFacade;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public RemoteGroupProjectMappingFacade() {
    super(RemoteGroupProjectMapping.class);
  }
  
  public RemoteGroupProjectMapping findByGroupAndProject(String group, Project project) {
    try {
      return em.createNamedQuery("RemoteGroupProjectMapping.findByGroupAndProject", RemoteGroupProjectMapping.class)
        .setParameter("remoteGroup", group).setParameter("project", project).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public List<RemoteGroupProjectMapping> findByGroup(String group) {
    return em.createNamedQuery("RemoteGroupProjectMapping.findByGroup", RemoteGroupProjectMapping.class)
      .setParameter("remoteGroup", group).getResultList();
  }
  
  public CollectionInfo<RemoteGroupProjectMapping> findAll(Integer offset, Integer limit,
    Set<? extends FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = buildQuery("SELECT l FROM RemoteGroupProjectMapping l ", filter, sort, null);
    String queryCountStr = buildQuery("SELECT COUNT(l.id) FROM RemoteGroupProjectMapping l ", filter, sort, null);
    Query query = em.createQuery(queryStr, RemoteGroupProjectMapping.class);
    Query queryCount = em.createQuery(queryCountStr, RemoteGroupProjectMapping.class);
    return findAll(offset, limit, filter, query, queryCount);
  }
  
  private CollectionInfo<RemoteGroupProjectMapping> findAll(Integer offset, Integer limit,
    Set<? extends FilterBy> filter, Query query, Query queryCount) {
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
    switch (RemoteGroupProjectMappingFacade.Filters.valueOf(filterBy.getValue())) {
      case ID:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      case PROJECT_ID:
        q.setParameter(filterBy.getField(), projectFacade.find(getIntValue(filterBy)));
        break;
      case REMOTE_GROUP:
      case PROJECT_ROLE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }
  
  public enum Sorts {
    ID("ID", "l.id ", "ASC"),
    PROJECT_ROLE("PROJECT_ROLE", "l.projectRole ", "ASC"),
    REMOTE_GROUP("REMOTE_GROUP", "l.remoteGroup ", "ASC");
    
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
    ID("ID", "l.id = :id ", "id", "0"),
    PROJECT_ID("PROJECT_ID", "l.projectId = :projectId ", "projectId", "0"),
    PROJECT_ROLE("PROJECT_ROLE", "l.projectRole = :projectRole ", "projectRole", ""),
    REMOTE_GROUP("REMOTE_GROUP", "l.remoteGroup = :remoteGroup ", "remoteGroup", "");
    
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
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
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
