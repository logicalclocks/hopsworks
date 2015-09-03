package se.kth.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.Metadata;
import se.kth.meta.entity.MetadataPK;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class MetadataFacade extends AbstractFacade<Metadata> {

  private static final Logger logger = Logger.getLogger(MetadataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MetadataFacade() {
    super(Metadata.class);
  }

  public Metadata getMetadata(MetadataPK metadataPK) throws DatabaseException {

    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findByPrimaryKey",
            Metadata.class);
    q.setParameter("metadataPK", metadataPK);

    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public Metadata getMetadataById(int id){
    
    TypedQuery<Metadata> q = this.em.createNamedQuery(
            "Metadata.findById", Metadata.class);
    q.setParameter("id", id);
    
    try{
      return q.getSingleResult();
    }catch(NoResultException e){
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
  public void addMetadata(Metadata metadata) throws DatabaseException {

    try {
      Metadata m = this.contains(metadata) ? metadata : this.getMetadata(
              metadata.getMetadataPK());

      if (m != null && m.getMetadataPK().getTupleid() != -1
              && m.getMetadataPK().getFieldid() != -1) {
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
  public boolean contains(Metadata metadata) {
    return this.em.contains(metadata);
  }
}