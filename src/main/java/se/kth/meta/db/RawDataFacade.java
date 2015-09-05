package se.kth.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.RawDataPK;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class RawDataFacade extends AbstractFacade<RawData> {

  private static final Logger logger = Logger.getLogger(RawDataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RawDataFacade() {
    super(RawData.class);
  }

  public RawData getRawData(RawDataPK rawdataPK) throws DatabaseException {

    TypedQuery<RawData> q = this.em.createNamedQuery("RawData.findByPrimaryKey",
            RawData.class);
    q.setParameter("rawdataPK", rawdataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'raw_data' table. RawData is the object that's
   * going to be persisted/updated in the database
   * <p>
   * @param raw
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addRawData(RawData raw) throws DatabaseException {

    try {
      RawData r = this.contains(raw) ? raw : this.getRawData(raw.getRawdataPK());

      if (r != null && r.getRawdataPK().getTupleid() != -1 && r.getRawdataPK().
              getFieldid() != -1) {
        /*
         * if the row exists just update it.
         */
        r.copy(raw);
        this.em.merge(r);
      } else {
        /*
         * if the row is new then just persist it
         */
        r = raw;
        this.em.persist(r);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException("Could not add raw data " + raw, e);
    }
  }

  public int getLastInsertedTupleId() throws DatabaseException {

    String queryString = "RawData.lastInsertedTupleId";

    Query query = this.em.createNamedQuery(queryString);
    List<RawData> list = query.getResultList();

    return (!list.isEmpty()) ? list.get(0).getId() : 0;
  }

  /**
   * Checks if a raw data instance is a managed entity
   * <p>
   * @param rawdata
   * @return
   */
  public boolean contains(RawData rawdata) {
    return this.em.contains(rawdata);
  }
}