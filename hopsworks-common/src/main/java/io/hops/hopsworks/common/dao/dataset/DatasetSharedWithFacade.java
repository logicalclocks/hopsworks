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
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Stateless
public class DatasetSharedWithFacade extends AbstractFacade<DatasetSharedWith> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  private InodeFacade inodeFacade;
  
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
    setFilter(filter, query, project);
    setFilter(filter, queryCount, project);
    setOffsetAndLim(offset, limit, query);
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }
  
  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q, Project project) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q, project);
    }
  }
  
  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q, Project project) {
    switch (Filters.valueOf(filterBy.getValue())) {
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      case ACCEPTED:
      case PUBLIC:
      case SHARED:
      case SEARCHABLE:
      case UNDER_CONSTRUCTION:
        q.setParameter(filterBy.getField(), getBooleanValue(filterBy.getParam()));
        break;
      case TYPE:
        q.setParameter(filterBy.getField(), getEnumValue(filterBy.getField(), filterBy.getValue(), DatasetType.class));
        break;
      case HDFS_USER:
        q.setParameter(filterBy.getField(), inodeFacade.gethdfsUser(filterBy.getParam()));
        break;
      case USER_EMAIL:
        q.setParameter(filterBy.getField(), inodeFacade.getUsers(filterBy.getParam(), project));
        break;
      case ACCESS_TIME:
      case ACCESS_TIME_GT:
      case ACCESS_TIME_LT:
      case MODIFICATION_TIME:
      case MODIFICATION_TIME_GT:
      case MODIFICATION_TIME_LT:
        Date date = getDate(filterBy.getField(), filterBy.getParam());
        q.setParameter(filterBy.getField(), date.getTime());
        break;
      case SIZE:
      case SIZE_LT:
      case SIZE_GT:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      default:
        break;
    }
  }
  
  public enum Sorts {
    ID("ID", "d.dataset.id ", "ASC"),
    NAME("NAME", "LOWER(d.dataset.name) ", "ASC"),
    SEARCHABLE("SEARCHABLE", "d.dataset.searchable ", "ASC"),
    MODIFICATION_TIME("MODIFICATION_TIME", "d.dataset.inode.modificationTime ", "ASC"),
    ACCESS_TIME("ACCESS_TIME", "d.dataset.inode.accessTime ", "ASC"),
    PUBLIC("PUBLIC", "d.dataset.public ", "ASC"),
    SIZE("SIZE", "d.dataset.inode.size ", "ASC"),
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
    NAME("NAME", "d.dataset.name LIKE CONCAT(:name, '%') ", "name", " "),
    USER_EMAIL("USER_EMAIL", "d.dataset.inode.hdfsUser =:user ", "user", " "),
    HDFS_USER("HDFS_USER", "d.dataset.inode.hdfsUser =:hdfsUser ", "hdfsUser", " "),
    UNDER_CONSTRUCTION("UNDER_CONSTRUCTION", "d.dataset.inode.underConstruction  =:underConstruction ",
      "underConstruction", "true"),
    ACCEPTED("ACCEPTED", "d.accepted =:accepted ", "accepted", "true"),
    SHARED("SHARED", "true =:shared ", "shared", "true"),//return all if true nothing if false
    SEARCHABLE("SEARCHABLE", "d.dataset.searchable =:searchable ", "searchable", "0"),
    TYPE("TYPE", "d.dataset.dsType =:dsType ", "dsType", "DATASET"),
    PUBLIC("PUBLIC", "d.dataset.public =:public ", "public", "0"),
    MODIFICATION_TIME("MODIFICATION_TIME", "d.dataset.inode.modificationTime  =:modificationTime ", "modificationTime",
      ""),
    MODIFICATION_TIME_LT("MODIFICATION_TIME_LT", "d.dataset.inode.modificationTime  <:modificationTime_lt ",
      "modificationTime_lt",
      ""),
    MODIFICATION_TIME_GT("MODIFICATION_TIME_GT", "d.dataset.inode.modificationTime  >:modificationTime_gt ",
      "modificationTime_gt",
      ""),
    ACCESS_TIME("ACCESS_TIME", "d.dataset.inode.accessTime  =:accessTime ", "accessTime", ""),
    ACCESS_TIME_LT("ACCESS_TIME_LT", "d.dataset.inode.accessTime  <:accessTime_lt ", "accessTime_lt", ""),
    ACCESS_TIME_GT("ACCESS_TIME_GT", "d.dataset.inode.accessTime  >:accessTime_gt ", "accessTime_gt", ""),
    SIZE("SIZE", "d.dataset.inode.size  =:size_eq ", "size_eq", "0"),
    SIZE_LT("SIZE_LT", "d.dataset.inode.size  <:size_lt ", "size_lt", "1"),
    SIZE_GT("SIZE_GT", "d.dataset.inode.size  >:size_gt ", "size_gt", "0");
    
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
