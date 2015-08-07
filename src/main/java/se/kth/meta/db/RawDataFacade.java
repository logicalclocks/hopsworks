package se.kth.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.RawData;
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

  public RawData getRawData(int rawid) throws DatabaseException {

    return this.em.find(RawData.class, rawid);
  }

  /**
   * adds a new record into 'raw_data' table. RawData is the object that's
   * going to be persisted to the database
   * <p>
   *
   * @param raw
   * @return the id of the affected row
   * @throws se.kth.meta.exception.DatabaseException
   */
  public int addRawData(RawData raw) throws DatabaseException {

    try {
      RawData r = this.contains(raw) ? raw : this.getRawData(raw.getId());

      if (raw.getId() != -1) {
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
      return r.getId();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(RawDataFacade.class.getName(), e.getMessage());
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
