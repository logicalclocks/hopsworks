package se.kth.meta.db;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
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
          
          this.em.persist(log);
          this.em.flush();
        } catch (PersistenceException e) {
          throw new DatabaseException(e.getMessage(), e);
        }
  }

  public boolean contains(HdfsMetadataLog hm) {
    return this.em.contains(hm);
  }
}
