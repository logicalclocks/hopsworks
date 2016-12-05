package se.kth.hopsworks.log.ops;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author Mahmoud Ismail<maism@kth.se>
 */
@Stateless
public class OperationsLogFacade extends AbstractFacade<OperationsLog> {

  private static final Logger logger = Logger.getLogger(OperationsLogFacade.class.
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
  
  public void flushEm(){
    em.flush();
  }
  
}
