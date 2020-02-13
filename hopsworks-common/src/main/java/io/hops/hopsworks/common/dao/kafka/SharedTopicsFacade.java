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
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.kafka.SharedTopics;
import io.hops.hopsworks.persistence.entity.kafka.SharedTopicsPK;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class SharedTopicsFacade extends AbstractFacade<SharedTopics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  public SharedTopicsFacade() {
    super(SharedTopics.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public List<SharedTopics> findSharedTopicsByProject(Integer projectId) {
    return em.createNamedQuery("SharedTopics.findByProjectId", SharedTopics.class)
      .setParameter("projectId", projectId)
      .getResultList();
  }
  
  public List<SharedTopics> findSharedTopicsByTopicName (String topicName) {
    return em.createNamedQuery("SharedTopics.findByTopicName", SharedTopics.class)
      .setParameter("topicName", topicName)
      .getResultList();
  }
  
  public Optional<SharedTopics> findSharedTopicByProjectAndTopic (Integer projectId, String topicName) {
    return Optional.ofNullable(em.find(SharedTopics.class,
      new SharedTopicsPK(topicName, projectId)));
  }
  
  public List<SharedTopics> findSharedTopicsByTopicAndOwnerProject (String topicName, Integer ownerProjectId) {
    return em.createNamedQuery(
      "SharedTopics.findByTopicAndOwnerProjectId", SharedTopics.class)
      .setParameter("topicName", topicName)
      .setParameter("ownerProjectId", ownerProjectId)
      .getResultList();
  }
  
  public void shareTopic(Project owningProject, String topicName, Integer projectId) {
    SharedTopics st = new SharedTopics(topicName, owningProject.getId(), projectId);
    em.persist(st);
    em.flush();
  }
  
  public Optional<SharedTopics> findSharedTopicByTopicAndProjectIds(String topicName, Integer ownerProjectId,
    Integer destProjectId) {
    try {
      return Optional.ofNullable(em.createNamedQuery("SharedTopics.findByTopicAndProjectsIds", SharedTopics.class)
        .setParameter("topicName", topicName)
        .setParameter("ownerProjectId", ownerProjectId)
        .setParameter("destProjectId", destProjectId)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public void acceptSharedTopic(Integer ownerProjectId, String topicName, Integer destProjectId) {
    findSharedTopicByTopicAndProjectIds(topicName, ownerProjectId, destProjectId)
      .ifPresent(st -> {
        st.setAccepted(true);
        update(st);
      });
  }
}
