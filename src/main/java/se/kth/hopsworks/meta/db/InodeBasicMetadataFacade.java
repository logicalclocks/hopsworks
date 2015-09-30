package se.kth.hopsworks.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.hopsworks.meta.entity.InodeBasicMetadata;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author vangelis
 */
@Stateless
public class InodeBasicMetadataFacade extends AbstractFacade<InodeBasicMetadata> {

  private static final Logger logger = Logger.getLogger(
          InodeBasicMetadataFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  public EntityManager getEntityManager() {
    return em;
  }

  public InodeBasicMetadataFacade() {
    super(InodeBasicMetadata.class);
  }

  public int addBasicMetadata(InodeBasicMetadata meta) {

    this.em.persist(meta);
    this.em.flush();
    this.em.clear();
    
    return meta.getId();
  }
}
