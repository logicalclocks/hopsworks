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

  public HdfsMetadataLog getHdfsMetadataLog(HdfsMetadataLog hm) throws
          DatabaseException {

    TypedQuery<HdfsMetadataLog> q = this.em.createNamedQuery(
            "HdfsMetadataLog.findByPrimaryKey",
            HdfsMetadataLog.class);
    q.setParameter("pk", hm.getHdfsMetadataLogPK());

    try {
      return q.getSingleResult();
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
      HdfsMetadataLog hm = this.getHdfsMetadataLog(log);

      if (hm != null) {
        int parentid = hm.getHdfsMetadataLogPK().getDatasetId();
        int inodeid = hm.getHdfsMetadataLogPK().getInodeid();
        int ltime = hm.getHdfsMetadataLogPK().getLtime();

        //try mutliple times until the logical time is the correct one
        while (true) {
          //increase the logical time until we have a valid entry
          ltime++;

          HdfsMetadataLog h = new HdfsMetadataLog(
                  new HdfsMetadataLogPK(parentid, inodeid, ltime), 0);

          if (this.getHdfsMetadataLog(h) == null) {
            break;
          }

          System.out.println("TRYING TO STORE " + h.getHdfsMetadataLogPK());
        }
        
        hm = new HdfsMetadataLog(new HdfsMetadataLogPK(parentid, inodeid, ltime), 0);
        this.em.persist(hm);
        this.em.flush();
        this.em.clear();
      }

    } catch (IllegalStateException | SecurityException ee) {

      throw new DatabaseException(ee.getMessage(), ee);
    }
  }

  public boolean contains(HdfsMetadataLog hm) {
    return this.em.contains(hm);
  }
}
