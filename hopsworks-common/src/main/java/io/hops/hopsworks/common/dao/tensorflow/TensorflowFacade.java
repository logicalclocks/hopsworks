package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;

@Stateless
public class TensorflowFacade {

  private final static Logger LOGGER = Logger.getLogger(TensorflowFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  private DistributedFsService dfs;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TensorflowFacade() throws Exception {
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

  public void createClusterInProject(Integer projectId, TfClusters cluster) {

    Project project = em.find(Project.class, projectId);
    if (project == null) {
      throw new NullPointerException("Project does not exist in database.");
    }

//    ProjectTopics pt = new ProjectTopics(topicName, projectId, schema);
//
//    em.persist(pt);
//    em.flush();
  }

  public void removeClusterFromProject(Project project, String clusterName) {

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
     * What is the possibility of the program failing below? The topic is
     * removed from
     * db, but not yet from zk. *
     * Possibilities:
     * 1. ZkClient is unable to establish a connection, maybe due to timeouts.
     * 2. In case delete.topic.enable is not set to true in the Kafka server
     * configuration, delete topic marks a topic for deletion. Subsequent
     * topic (with the same name) create operation fails.
     */
  }

  public String getTensorboardURI(String appId, String projectName) {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String tensorboardFile = File.separator + Settings.DIR_ROOT
            + File.separator + projectName + File.separator + Settings.PROJECT_STAGING_DIR + File.separator
            + ".tensorboard." + appId;
      try {
        FSDataInputStream file = dfso.open(tensorboardFile);
        String uri = IOUtils.toString(file);
        return uri;
      } catch (IOException ex) {
        LOGGER.log(Level.WARNING, "error while trying to read tensorboard file: " + tensorboardFile, ex);
        return null;
      }

    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
}
