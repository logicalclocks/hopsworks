package se.kth.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.MetaData;
import se.kth.meta.entity.MetaDataPK;
import se.kth.meta.entity.RawData;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class MetaDataFacade extends AbstractFacade<MetaData> {

  private static final Logger logger = Logger.getLogger(RawDataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MetaDataFacade() {
    super(MetaData.class);
  }

  public MetaData getMetaData(MetaDataPK metadataPK) throws DatabaseException {

    TypedQuery<MetaData> q = this.em.createNamedQuery(
            "MetaData.findByPrimaryKey",
            MetaData.class);
    q.setParameter("metadataPK", metadataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * adds a new record into 'meta_data' table. MetaData is the object that's
   * going to be persisted/updated in the database
   * <p>
   *
   * @param metadata
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addMetadata(MetaData metadata) throws DatabaseException {

    try {
      MetaData m = this.contains(metadata) ? metadata : this.getMetaData(
              metadata.getMetaDataPK());

      if (m != null && m.getMetaDataPK().getTupleid() != -1
              && m.getMetaDataPK().getFieldid() != -1) {
        /*
         * if the row exists just update it.
         */
        m.copy(metadata);
        this.em.merge(m);
      } else {
        /*
         * if the row is new then just persist it
         */
        m = metadata;
        this.em.persist(m);
      }

      this.em.flush();
      this.em.clear();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(RawDataFacade.class.getName(), e.getMessage());
    }
  }

  /**
   * Checks if a raw data instance is a managed entity
   * <p>
   * @param metadata
   * @return
   */
  public boolean contains(MetaData metadata) {
    return this.em.contains(metadata);
  }
}
