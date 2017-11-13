package io.hops.hopsworks.common.dao.yarn.containers;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class YarnContainersLogsFacade extends AbstractFacade<YarnContainersLog> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnContainersLogsFacade() {
    super(YarnContainersLog.class);
  }

  @Override
  public List<YarnContainersLog> findAll() {
    TypedQuery<YarnContainersLog> query = em.createNamedQuery("YarnContainersLogs.findAll",YarnContainersLog.class);
    return query.getResultList();
  }
  
  public List<YarnContainersLog> findAllRunningOnGpus() {
    TypedQuery<YarnContainersLog> query = em.createNamedQuery("YarnContainersLogs.findRunningOnGpu",
        YarnContainersLog.class);
    return query.getResultList();
  }
}
