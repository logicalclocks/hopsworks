package io.hops.hopsworks.common.dao.log.operation;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class OperationsLogFacade extends AbstractFacade<OperationsLog> {

  private static final Logger logger = Logger.getLogger(
          OperationsLogFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public OperationsLogFacade() {
    super(OperationsLog.class);
  }

  public OperationsLogFacade(Class<OperationsLog> entityClass) {
    super(entityClass);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public void persist(OperationsLog log) {
    em.persist(log);
  }

  public void flushEm() {
    em.flush();
  }

}
