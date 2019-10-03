package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
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
  
  public void unshareTopic(SharedTopics st) {
    em.remove(st);
  }
}
