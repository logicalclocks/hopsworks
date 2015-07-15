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

  /**
   * adds a new record into 'raw_data' table. RawData is the object that's
   * going to be persisted to the database
   * <p>
   *
   * @param raw
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addRawData(RawData raw) throws DatabaseException {

    this.em.persist(raw);
  }

  public int getLastInsertedTupleId() throws DatabaseException {

    String queryString = "RawData.lastInsertedTupleId";

    Query query = this.em.createNamedQuery(queryString);
    List<RawData> list = query.getResultList();

    return (!list.isEmpty()) ? list.get(0).getId() : 0;
  }
}
