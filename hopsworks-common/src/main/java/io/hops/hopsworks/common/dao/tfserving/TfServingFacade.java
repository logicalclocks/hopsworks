package io.hops.hopsworks.common.dao.tfserving;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.TensorflowFacade;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class TfServingFacade {
  private final static Logger LOGGER = Logger.getLogger(TensorflowFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TfServingFacade() throws Exception {

  }

  public void persist(TfServing tfServing) throws DatabaseException {
    try {
      em.persist(tfServing);
    } catch (ConstraintViolationException cve) {
      throw new DatabaseException("You can not create a serving with the same name as an existing one");
    }
  }

  public List<TfServing> findForProject(Project project) {
    TypedQuery<TfServing> q = em.createNamedQuery("TfServing.findByProject", TfServing.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public boolean updateServingVersion(TfServing tfServing) throws DatabaseException {
    boolean status = false;
    try {
      TypedQuery<TfServing> q = em.createNamedQuery("TfServing.updateModelVersion", TfServing.class);
      q.setParameter("id", tfServing.getId());
      q.setParameter("version", tfServing.getVersion());
      q.setParameter("hdfsModelPath", tfServing.getHdfsModelPath());

      int result = q.executeUpdate();
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      throw new DatabaseException("Could not update serving  ", ex);
    }
    return status;
  }

  public void remove(TfServing tfServing) throws DatabaseException {
    try {
      TfServing managedTfServing = em.find(TfServing.class, tfServing.getId());
      em.remove(em.merge(managedTfServing));
      em.flush();
    } catch (SecurityException | IllegalStateException ex) {
      throw new DatabaseException("Could not delete serving " + tfServing.getId(), ex);
    }
  }

  public TfServing findById(Integer id) {
    return em.find(TfServing.class, id);
  }

  public boolean updateRunningState(TfServing tfServing) throws DatabaseException {
    boolean status = false;
    try {
      TypedQuery<TfServing> q = em.createNamedQuery("TfServing.updateRunningState", TfServing.class);
      q.setParameter("id", tfServing.getId());
      q.setParameter("pid", tfServing.getPid());
      q.setParameter("port", tfServing.getPort());
      q.setParameter("hostIp", tfServing.getHostIp());
      q.setParameter("status", tfServing.getStatus());

      int result = q.executeUpdate();
      if (result == 1) {
        status = true;
      }
    } catch (SecurityException | IllegalArgumentException ex) {
      throw new DatabaseException("Could not update serving  ", ex);
    }
    return status;
  }
}
