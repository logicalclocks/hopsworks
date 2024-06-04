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
package io.hops.hopsworks.common.dao.dataset;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

@Stateless
public class DatasetSharedWithFacade extends AbstractFacade<DatasetSharedWith> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public DatasetSharedWithFacade() {
    super(DatasetSharedWith.class);
  }
  
  public List<DatasetSharedWith> findByDataset(Dataset dataset) {
    return em.createNamedQuery("DatasetSharedWith.findByDataset", DatasetSharedWith.class)
      .setParameter("dataset", dataset).getResultList();
  }
  
  public List<DatasetSharedWith> findByProject(Project project) {
    return em.createNamedQuery("DatasetSharedWith.findByProject", DatasetSharedWith.class)
      .setParameter("project", project).getResultList();
  }
  
  public DatasetSharedWith findByProjectAndDataset(Project project, Dataset dataset) {
    try {
      return em.createNamedQuery("DatasetSharedWith.findByProjectAndDataset", DatasetSharedWith.class)
        .setParameter("project", project).setParameter("dataset", dataset).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public CollectionInfo findAllDatasetByProject(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT d FROM DatasetSharedWith d ", filter, sort, "d.project = :project ");
    String queryCountStr = buildQuery("SELECT COUNT(DISTINCT d.id) FROM DatasetSharedWith d ", filter, null,
      "d.project = :project ");
    Query query = em.createQuery(queryStr, DatasetSharedWith.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, DatasetSharedWith.class).setParameter("project", project);
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
      case NAME:
      case NAME_NEQ:
      case NAME_LIKE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case ACCEPTED:
      case PUBLIC:
      case SHARED:
      case SEARCHABLE:
        q.setParameter(filterBy.getField(), getBooleanValue(filterBy.getParam()));
        break;
      case TYPE:
        q.setParameter(filterBy.getField(), getEnumValue(filterBy.getField(), filterBy.getValue(), DatasetType.class));
        break;
      default:
        break;
    }
  }
  
  public enum Sorts {
    ID("ID", "d.dataset.id ", "ASC"),
    NAME("NAME", "LOWER(d.dataset.name) ", "ASC"),
    SEARCHABLE("SEARCHABLE", "d.dataset.searchable ", "ASC"),
    PUBLIC("PUBLIC", "d.dataset.public ", "ASC"),

    TYPE("TYPE", "d.dataset.dsType ", "ASC");
    
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
    
    public String getJoin(){
      return null;
    }
    
    @Override
    public String toString() {
      return value;
    }
    
  }
  
  public enum Filters {
    NAME("NAME", "LOWER(d.dataset.name) LIKE LOWER(CONCAT(:name, '%')) ", "name", " "),
    NAME_NEQ("NAME_NEQ", "LOWER(d.dataset.name) NOT LIKE LOWER(CONCAT(:name_not_eq, '%')) ", "name_not_eq", " "),
    NAME_LIKE("NAME_LIKE", "LOWER(d.dataset.name) LIKE LOWER(CONCAT('%', :name_like, '%')) ", "name_like", " "),
    ACCEPTED("ACCEPTED", "d.accepted =:accepted ", "accepted", "true"),
    SHARED("SHARED", "true =:shared ", "shared", "true"),//return all if true nothing if false
    SEARCHABLE("SEARCHABLE", "d.dataset.searchable =:searchable ", "searchable", "0"),
    TYPE("TYPE", "d.dataset.dsType =:dsType ", "dsType", "DATASET"),
    PUBLIC("PUBLIC", "d.dataset.public =:public ", "public", "0");
    
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
