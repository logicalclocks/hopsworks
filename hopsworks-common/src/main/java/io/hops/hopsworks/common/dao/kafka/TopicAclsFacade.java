/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.kafka.TopicAcls;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class TopicAclsFacade extends AbstractFacade<TopicAcls> {
  
  public TopicAclsFacade() {
    super(TopicAcls.class);
  }
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  private static final Logger LOGGER = Logger.getLogger(TopicAclsFacade.class.getName());
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public void removeAclFromTopic(String topicName, Project project) {
    findByTopicName(topicName)
      .stream()
      .filter(acl -> KafkaConst.getProjectNameFromPrincipal(acl.getPrincipal()).equals(project.getName()))
      .forEach(this::remove);
  }
  
  public List<TopicAcls> findByTopicName(String topicName) {
    return em.createNamedQuery(
      "TopicAcls.findByTopicName", TopicAcls.class)
      .setParameter("topicName", topicName)
      .getResultList();
  }
  
  public Optional<TopicAcls> getTopicAcls(String topicName, String principal, String permission_type,
    String operation_type, String host, String role) {
    
    try {
      return Optional.ofNullable(em.createNamedQuery(
        "TopicAcls.findAcl", TopicAcls.class)
        .setParameter("topicName", topicName)
        .setParameter("principal", principal)
        .setParameter("role", role)
        .setParameter("host", host)
        .setParameter("operationType", operation_type)
        .setParameter("permissionType", permission_type)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<TopicAcls> getTopicAcls(String topicName, AclDTO dto, String principalName) {
    return getTopicAcls(topicName, principalName, dto.getPermissionType(), dto.getOperationType(), dto.getHost(), dto
      .getRole());
  }
  
  public void removeAclsForUser(Users user, Project project) {
    em.createNamedQuery("TopicAcls.deleteByUser", TopicAcls.class)
      .setParameter("user", user)
      .setParameter("project", project)
      .executeUpdate();
  }
  
  public void removeAclsForUserAndPrincipalProject(Users user, String projectName) {
    em.createNamedQuery("TopicAcls.deleteByUserAndPrincipalProject", TopicAcls.class)
      .setParameter("user", user)
      .setParameter("project", projectName)
      .executeUpdate();
  }
  
  
  public void removeAclForProject(Project project) {
    em.createNamedQuery("TopicAcls.findAll", TopicAcls.class)
      .getResultList()
      .stream()
      .filter(acl -> KafkaConst.getProjectNameFromPrincipal(acl.getPrincipal()).equals(project.getName()))
      .forEach(acl -> em.remove(acl));
  }
  
  public TopicAcls addAclsToTopic(ProjectTopics pt, Users user, String permissionType, String operationType,
    String host, String role, String principalName) {
    
    TopicAcls ta = new TopicAcls(pt, user, permissionType, operationType, host, role, principalName);
    save(ta);
    em.flush();
    return ta;
  }
  
  public CollectionInfo findByTopicName(Integer offset, Integer limit,
    Set<? extends FilterBy> filters,
    Set<? extends AbstractFacade.SortBy> sorts, String topicName) {
  
    String queryStr = buildQuery("SELECT t FROM TopicAcls t ", filters,
      sorts, " t.projectTopics.topicName = :topicName ");
    String queryCountStr =
      buildQuery("SELECT COUNT(DISTINCT t.id) FROM TopicAcls t ",
        filters,
        sorts,
        "t.projectTopics.topicName = :topicName ");
    Query query = em.createQuery(queryStr, TopicAcls.class).setParameter("topicName", topicName);
    Query queryCount = em.createQuery(queryCountStr, TopicAcls.class).setParameter("topicName", topicName);
    setFilter(filters, query);
    setFilter(filters, queryCount);
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
    switch (TopicAclsFilters.valueOf(filterBy.getValue())) {
      case ID:
        q.setParameter(filterBy.getField(), Integer.valueOf(filterBy.getParam()));
        break;
      case HOST:
      case USER_EMAIL:
      case PROJECT_NAME:
      case OPERATION_TYPE:
      case PERMISSION_TYPE:
      case ROLE:
        q.setParameter(filterBy.getField(), filterBy.getParam());
        break;
      default:
        break;
    }
  }
  
  public enum TopicAclsSorts {
    
    ID("ID", "t.id ", "ASC"),
    HOST("HOST","LOWER(t.host) ","ASC"),
    OPERATION_TYPE("OPERATION_TYPE","LOWER(t.operationType) ","ASC"),
    PERMISSION_TYPE("PERMISSION_TYPE","LOWER(t.permissionType) ","ASC"),
    PROJECT_NAME("PROJECT_NAME","LOWER(t.principal) ","ASC"),
    ROLE("ROLE","LOWER(t.role) ","ASC"),
    USER_EMAIL("USER_EMAIL","LOWER(t.user.email) ","ASC");
  
    private final String value;
    private final String sql;
    private final String defaultParam;
  
    private TopicAclsSorts(String value, String sql, String defaultParam) {
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
  
  public enum TopicAclsFilters {
  
    ID("ID", "t.id = :id", "id", ""),
    HOST("HOST", "t.host = :host", "host", ""),
    OPERATION_TYPE("OPERATION_TYPE", "t.operationType = :operationType", "operationType", ""),
    PERMISSION_TYPE("PERMISSION_TYPE", "t.permissionType = :permissionType", "permissionType", ""),
    PROJECT_NAME("PROJECT_NAME", "t.principal LIKE CONCAT(:projectName,'%') ", "projectName", ""),
    ROLE("ROLE", "t.role = :role", "role", ""),
    USER_EMAIL("USER_EMAIL", "t.user.email = :userEmail", "userEmail", "");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
  
    private TopicAclsFilters(String value, String sql, String field, String defaultParam) {
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
