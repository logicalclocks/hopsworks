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
  
  public void updateTopicSchemaVersion(ProjectTopics pt, SchemaTopics st) {
    pt.setSchemaTopics(new SchemaTopics(st.schemaTopicsPK.getName(), st.schemaTopicsPK.getVersion()));
    update(pt);
  }
}
