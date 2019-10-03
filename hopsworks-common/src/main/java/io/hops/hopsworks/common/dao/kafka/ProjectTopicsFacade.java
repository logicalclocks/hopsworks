package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;

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
  
  public List<ProjectTopics> findTopicsByProject(Project project, Integer offset, Integer limit,
    Set<? extends FilterBy> filter, Set<? extends AbstractFacade.SortBy> sort) {
    String queryString = buildQuery(
      "SELECT a.* FROM (SELECT p.*, TRUE AS is_shared FROM ProjectTopics p " +
        "WHERE EXISTS (SELECT * FROM SharedTopics s " +
        "WHERE s.project_id = :projectId AND p.topic_name = s.topic_name) \n" +
        "UNION \n" +
        "SELECT p.*, FALSE AS is_shared FROM ProjectTopics p WHERE p.project_id = :projectId) a ",
      filter,
      sort,
      "");
    Query query = em.createQuery(queryString).setParameter("projectId", project.getId());
    setOffsetAndLim(offset, limit, query);
    List list = query.getResultList();
    return list;
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
