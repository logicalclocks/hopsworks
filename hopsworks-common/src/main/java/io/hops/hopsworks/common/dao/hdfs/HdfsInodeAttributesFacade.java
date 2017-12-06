package io.hops.hopsworks.common.dao.hdfs;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class HdfsInodeAttributesFacade extends AbstractFacade<HdfsInodeAttributes> {

  private final static Logger logger = Logger.getLogger(HdfsInodeAttributes.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsInodeAttributesFacade() { super(HdfsInodeAttributes.class); }

  public HdfsInodeAttributes getInodeAttributes(int inodeId) {
    TypedQuery<HdfsInodeAttributes> query = em.createNamedQuery(
        "HdfsInodeAttributes.findByInodeId", HdfsInodeAttributes.class)
        .setParameter("inodeId", inodeId);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      logger.log(Level.SEVERE, "Cannot find Inode attributes for inode: " + inodeId);
    }

    return null;
  }

}
