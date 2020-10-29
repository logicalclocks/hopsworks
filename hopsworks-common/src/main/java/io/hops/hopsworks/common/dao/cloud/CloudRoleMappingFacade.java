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
package io.hops.hopsworks.common.dao.cloud;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMappingDefault;
import io.hops.hopsworks.persistence.entity.cloud.ProjectRoles;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class CloudRoleMappingFacade extends AbstractFacade<CloudRoleMapping> {
  private static final Logger LOGGER = Logger.getLogger(CloudRoleMappingFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  @EJB
  ProjectFacade projectFacade;

  public CloudRoleMappingFacade() {
    super(CloudRoleMapping.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<CloudRoleMapping> findAllOrderById() {
    TypedQuery<CloudRoleMapping> query = em.createNamedQuery("CloudRoleMapping.findAll", CloudRoleMapping.class);
    return query.getResultList();
  }

  public List<CloudRoleMapping> findByCloudRole(String cloudRole) {
    TypedQuery<CloudRoleMapping> query = em.createNamedQuery("CloudRoleMapping.findByCloudRole", CloudRoleMapping.class)
      .setParameter("cloudRole", cloudRole);
    return query.getResultList();
  }

  public CloudRoleMapping findByProjectAndCloudRole(Project project, String cloudRole) {
    TypedQuery<CloudRoleMapping> query =
      em.createNamedQuery("CloudRoleMapping.findByProjectAndCloudRole", CloudRoleMapping.class)
        .setParameter("projectId", project)
        .setParameter("cloudRole", cloudRole);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public CloudRoleMapping findByIdAndProject(Integer id, Project project) {
    TypedQuery<CloudRoleMapping> query =
      em.createNamedQuery("CloudRoleMapping.findByIdAndProject", CloudRoleMapping.class)
        .setParameter("id", id)
        .setParameter("projectId", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public CloudRoleMapping findDefaultForRole(Project project, ProjectRoles projectRole) {
    TypedQuery<CloudRoleMappingDefault> query =
      em.createNamedQuery("CloudRoleMappingDefault.findDefaultByProjectAndProjectRole", CloudRoleMappingDefault.class)
        .setParameter("projectId", project)
        .setParameter("projectRole", projectRole.getDisplayName());
    try {
      return query.getSingleResult().getCloudRoleMapping();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void changeDefault(CloudRoleMapping fromCloudRoleMapping, CloudRoleMapping toCloudRoleMapping) {
    CloudRoleMappingDefault cloudRoleMappingDefault = fromCloudRoleMapping.getCloudRoleMappingDefault();
    cloudRoleMappingDefault.setCloudRoleMapping(toCloudRoleMapping);
    em.merge(em.merge(cloudRoleMappingDefault));
  }

  public void setDefault(CloudRoleMapping cloudRoleMapping, boolean defaultRole) {
    CloudRoleMappingDefault cloudRoleMappingDefault;
    if (defaultRole) {
      cloudRoleMappingDefault = new CloudRoleMappingDefault(cloudRoleMapping);
      em.persist(cloudRoleMappingDefault);
    } else if (cloudRoleMapping.getCloudRoleMappingDefault() != null) {
      cloudRoleMappingDefault = cloudRoleMapping.getCloudRoleMappingDefault();
      em.remove(em.merge(cloudRoleMappingDefault));
    }
  }

  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort) {
    String queryStr = buildQuery("SELECT c FROM CloudRoleMapping c ", filter, sort, null);
    String queryCountStr = buildQuery("SELECT COUNT(c.id) FROM CloudRoleMapping c ", filter, sort, null);
    Query query = em.createQuery(queryStr, CloudRoleMapping.class);
    Query queryCount = em.createQuery(queryCountStr, CloudRoleMapping.class);
    return findAll(offset, limit, filter, query, queryCount);
  }

  public CollectionInfo findAll(Integer offset, Integer limit, Set<? extends FilterBy> filter,
    Set<? extends AbstractFacade.SortBy> sort, Project project) {
    String queryStr = buildQuery("SELECT c FROM CloudRoleMapping c ", filter, sort, "c.projectId = :project");
    String queryCountStr =
      buildQuery("SELECT COUNT(c.id) FROM CloudRoleMapping c ", filter, sort, "c.projectId = :project");
    Query query = em.createQuery(queryStr, CloudRoleMapping.class).setParameter("project", project);
    Query queryCount = em.createQuery(queryCountStr, CloudRoleMapping.class).setParameter("project", project);
    return findAll(offset, limit, filter, query, queryCount);
  }

  private CollectionInfo findAll(Integer offset, Integer limit,
    Set<? extends AbstractFacade.FilterBy> filter, Query query, Query queryCount) {
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
      case ID_GT:
      case ID_LT:
        q.setParameter(filterBy.getField(), getIntValue(filterBy));
        break;
      case PROJECT_ID:
        q.setParameter(filterBy.getField(), projectFacade.find(getIntValue(filterBy)));
        break;
      case CLOUD_ROLE:
      case PROJECT_ROLE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    ID("ID", "c.id ", "ASC"),
    PROJECT_ID("PROJECT_ID", "c.projectId ", "ASC"),
    PROJECT_ROLE("PROJECT_ROLE", "c.projectRole ", "ASC"),
    CLOUD_ROLE("CLOUD_ROLE", "c.cloudRole ", "ASC");

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
    ID("ID", "c.id = :id ", "id", "0"),
    ID_LT("ID_LT", "c.id < :id_lt ", "id_lt", "0"),
    ID_GT("ID_GT", "c.id > :id_gt ", "id_gt", "0"),
    PROJECT_ID("PROJECT_ID", "c.projectId = :projectId ", "projectId", "0"),
    PROJECT_ROLE("PROJECT_ROLE", "c.projectRole = :projectRole ", "projectRole", ""),
    CLOUD_ROLE("CLOUD_ROLE", "c.cloudRole = :cloudRole ", "cloudRole", "");

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
