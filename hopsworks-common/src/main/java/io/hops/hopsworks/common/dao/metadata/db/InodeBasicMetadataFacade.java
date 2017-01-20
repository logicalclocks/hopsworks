package io.hops.hopsworks.common.dao.metadata.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.metadata.InodeBasicMetadata;
import io.hops.hopsworks.common.dao.AbstractFacade;

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

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public int addBasicMetadata(InodeBasicMetadata meta) {

    this.em.persist(meta);
    this.em.flush();
    this.em.clear();

    return meta.getId();
  }
}
