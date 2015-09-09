package se.kth.meta.db;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.HdfsMetadataLog;
import se.kth.meta.entity.HdfsMetadataLogPK;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class HdfsMetadataLogFacade extends AbstractFacade<HdfsMetadataLog> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HdfsMetadataLogFacade() {
    super(HdfsMetadataLog.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return this.em;
  }

  public HdfsMetadataLog getMostRecentMetaLog(HdfsMetadataLog hm) throws
          DatabaseException {

    TypedQuery<HdfsMetadataLog> q = this.em.createNamedQuery(
            "HdfsMetadataLog.findMostRecentMutation",
            HdfsMetadataLog.class);
    
    q.setParameter("datasetid", hm.getHdfsMetadataLogPK().getDatasetId());
    q.setParameter("inodeid", hm.getHdfsMetadataLogPK().getInodeid());

    try {
      return (!q.getResultList().isEmpty()) ? q.getResultList().get(0) : null;
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'meta_data' table. MetaData is the object that's
   * going to be persisted/updated in the database
   * <p/>
   *
   * @param log
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addHdfsMetadataLog(HdfsMetadataLog log) throws DatabaseException {

    try {
      HdfsMetadataLog hm = this.getMostRecentMetaLog(log);
      HdfsMetadataLog toSave;

      if (hm != null) {
        int parentid = hm.getHdfsMetadataLogPK().getDatasetId();
        int inodeid = hm.getHdfsMetadataLogPK().getInodeid();
        
        //increase the logical time to be valid
        int ltime = hm.getHdfsMetadataLogPK().getLtime() + 1;

        toSave = new HdfsMetadataLog(
                new HdfsMetadataLogPK(parentid, inodeid, ltime), 0);
      } else {
        toSave = new HdfsMetadataLog(log);
      }

      this.em.persist(toSave);
      this.em.flush();
      this.em.clear();
      
    } catch (IllegalStateException | SecurityException ee) {

      throw new DatabaseException(ee.getMessage(), ee);
    }
  }

  public boolean contains(HdfsMetadataLog hm) {
    return this.em.contains(hm);
  }
}
