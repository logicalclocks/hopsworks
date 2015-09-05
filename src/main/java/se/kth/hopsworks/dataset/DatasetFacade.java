package se.kth.hopsworks.dataset;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author ermiasg
 */
@Stateless
public class DatasetFacade extends AbstractFacade<Dataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetFacade() {
    super(Dataset.class);
  }

  @Override
  public List<Dataset> findAll() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAll",
            Dataset.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p>
   * @param id
   * @return
   */
  public Dataset find(Integer id) {
    return em.find(Dataset.class, id);
  }

  /**
   * Finds all instances of a dataset. i.e if a dataset is shared it is going
   * to be present in the parent project and in the project it is shard with.
   * <p>
   * @param inode
   * @return
   */
  public List<Dataset> findByInode(Inode inode) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInode",
            Dataset.class).setParameter(
                    "inode", inode);
    return query.getResultList();
  }

  /**
   * Find by project and dataset name
   * <p>
   * @param project
   * @param inode
   * @return
   */
  public Dataset findByProjectAndInode(Project project, Inode inode) {
    try {
      return em.createNamedQuery("Dataset.findByProjectAndInode", Dataset.class)
              .setParameter("projectId", project).setParameter(
                      "inode", inode).getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Finds all data sets in a project.
   * <p>
   * @param project
   * @return
   */
  public List<Dataset> findByProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByProject",
            Dataset.class).setParameter(
                    "projectId", project);
    return query.getResultList();
  }

  public void persistDataset(Dataset dataset) {
    em.persist(dataset);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(Dataset dataset) {
    em.merge(dataset);
  }
}
