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
package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.schemas.Subjects;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class ProjectTopicsFacade extends AbstractFacade<ProjectTopics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public ProjectTopicsFacade() {
    super(ProjectTopics.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  private static final Logger LOGGER = Logger.getLogger(ProjectTopicsFacade.class.getName());
  
  public List<ProjectTopics> findTopicsByProject (Project project) {
    return em.createNamedQuery("ProjectTopics.findByProject", ProjectTopics.class)
      .setParameter("project", project)
      .getResultList();
  }
  
  public Optional<ProjectTopics> findTopicByNameAndProject(Project project, String topicName) {
    try {
      return Optional.of(em.createNamedQuery("ProjectTopics.findByProjectAndTopicName", ProjectTopics.class)
        .setParameter("project", project)
        .setParameter("topicName", topicName)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<ProjectTopics> findTopicByName(String topicName) {
    try {
      return Optional.of(em.createNamedQuery("ProjectTopics.findByTopicName", ProjectTopics.class)
        .setParameter("topicName", topicName)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public List<ProjectTopics> findTopiscBySubjectAndVersion(Project project, String subject, Integer version) {
    return em.createNamedQuery("ProjectTopics.findBySubjectAndVersion", ProjectTopics.class)
      .setParameter("project", project)
      .setParameter("subject", subject)
      .setParameter("version", version)
      .getResultList();
  }
  
  public List<ProjectTopics> findTopicsBySubject(Project project, String subject) {
    return em.createNamedQuery("ProjectTopics.findBySubject", ProjectTopics.class)
      .setParameter("project", project)
      .setParameter("subject", subject)
      .getResultList();
  }
  
  public void updateTopicSchemaVersion(ProjectTopics pt, Subjects st) {
    pt.setSubjects(st);
    update(pt);
  }
  
  public enum TopicsSorts {
    NAME("NAME", "LOWER(t.name)", "ASC"),
    SCHEMA_NAME("SCHEMA_NAME", "LOWER(t.schemaName)", "ASC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    private TopicsSorts(String value, String sql, String defaultParam) {
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
  
  public enum TopicsFilters {
    SHARED("SHARED", "t.isShared = :shared", "shared", "false");
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private TopicsFilters(String value, String sql, String field, String defaultParam) {
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
