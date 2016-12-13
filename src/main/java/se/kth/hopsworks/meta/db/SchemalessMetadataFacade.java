package se.kth.hopsworks.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.fb.Inode;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.meta.entity.SchemalessMetadata;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author Mahmoud Ismail<maism@kth.se>
 */
@Stateless
public class SchemalessMetadataFacade extends AbstractFacade<SchemalessMetadata> {

  private static final Logger logger = Logger.getLogger(SchemalessMetadataFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;


  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public SchemalessMetadataFacade() {
    super(SchemalessMetadata.class);
  }

  public SchemalessMetadata findByInode(Inode inode) {
    TypedQuery<SchemalessMetadata> query = em.createNamedQuery("SchemalessMetadata.findByInode",
            SchemalessMetadata.class);
    query.setParameter("inode", inode);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public void persist(SchemalessMetadata metadata){
    em.persist(metadata);
  }
  
  public void merge(SchemalessMetadata metadata) {
    em.merge(metadata);
    em.flush();
  }
  
  public void remove(SchemalessMetadata metadata) {
    em.remove(metadata);
  }

}
