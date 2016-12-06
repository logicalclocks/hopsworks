package io.hops.tf;

import se.kth.bbc.project.*;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.util.Settings;

@Stateless
public class TensorflowFacade {

  private final static Logger LOGGER = Logger.getLogger(TensorflowFacade.class.
    getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TensorflowFacade() throws AppException, Exception {
  }

  /**
   * Get all the Topics for the given project.
   * <p/>
   * @param projectId
   * @return
   */
  public List<TfClusters> findClustersByProject(Integer projectId) {
//    TypedQuery<ProjectTopics> query = em.createNamedQuery(
//            "ProjectTopics.findByProjectId",
//            ProjectTopics.class);
//    query.setParameter("projectId", projectId);
//    List<ProjectTopics> res = query.getResultList();
//    List<TopicDTO> topics = new ArrayList<>();
//    for (ProjectTopics pt : res) {
//      topics.add(new TopicDTO(pt.getProjectTopicsPK().getTopicName()));
//    }
//    return topics;
    return null;
  }

  public void createClusterInProject(Integer projectId, TfClusters cluster)
    throws AppException {

    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
        "Project does not exist in database.");
    }

//    ProjectTopics pt = new ProjectTopics(topicName, projectId, schema);
//
//    em.persist(pt);
//    em.flush();
  }

  public void removeClusterFromProject(Project project, String clusterName)
    throws AppException {

//    ProjectTopics pt = em.find(ProjectTopics.class,
//            new ProjectTopicsPK(topicName, project.getId()));
//
//    if (pt == null) {
//      throw new AppException(Response.Status.FOUND.getStatusCode(),
//              "Kafka topic does not exist in database.");
//    }
//
//    //remove from database
//    em.remove(pt);
    /*
        What is the possibility of the program failing below? The topic is removed from
        db, but not yet from zk. 
        
        Possibilities:
            1. ZkClient is unable to establish a connection, maybe due to timeouts.
            2. In case delete.topic.enable is not set to true in the Kafka server 
               configuration, delete topic marks a topic for deletion. Subsequent 
               topic (with the same name) create operation fails. 
     */
  }
}
