package io.hops.hopsworks.common.dao.pysparkDeps;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;

@Stateless
public class PythonDepFacade {

  private final static Logger LOGGER = Logger.getLogger(PythonDepFacade.class.
    getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;

  protected EntityManager getEntityManager() {
    return em;
  }

  public PythonDepFacade() throws Exception {
  }

  /**
   * Get all the Topics for the given project.
   * <p/>
   * @param projectId
   * @return
   */
  public List<PythonDep> findPythonDepByProject(Integer projectId) {
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

  public void installPythonDepToProject(Integer projectId, String dependency, String anacondaRepoUrl) {

    // test if anacondaRepoUrl exists. If not, add it.
    // Test if pythonDep exists. If not, add it.
  }

  public void removePythonDepFromProject(Integer projectId, String dependency) {

  }

}
