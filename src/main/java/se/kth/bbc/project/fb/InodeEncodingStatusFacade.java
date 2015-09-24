package se.kth.bbc.project.fb;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author vangelis
 */
public class InodeEncodingStatusFacade extends AbstractFacade<InodeEncodingStatus> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return this.em;
  }

  public InodeEncodingStatusFacade() {
    super(InodeEncodingStatus.class);
  }

  public InodeEncodingStatus findById(Integer inodeid) {
    TypedQuery<InodeEncodingStatus> q = em.createNamedQuery("InodeEncodingStatus.findById",
            InodeEncodingStatus.class);
    q.setParameter("id", inodeid);
    
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
