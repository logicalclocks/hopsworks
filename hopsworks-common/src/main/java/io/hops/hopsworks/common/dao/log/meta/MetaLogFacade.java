package io.hops.hopsworks.common.dao.log.meta;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class MetaLogFacade extends AbstractFacade<MetaLog> {

  private static final Logger logger = Logger.getLogger(MetaLogFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public MetaLogFacade() {
    super(MetaLog.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public void persist(MetaLog metaLog) {
    em.persist(metaLog);
  }

}
