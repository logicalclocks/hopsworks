package se.kth.hopsworks.users;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.Userlogins;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author Ermias
 */
@Stateless
public class UserLoginsFacade extends AbstractFacade<Userlogins> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public UserLoginsFacade() {
    super(Userlogins.class);
  }

  @Override
  public List<Userlogins> findAll() {
    TypedQuery<Userlogins> query = em.createNamedQuery("Userlogins.findAll",
            Userlogins.class);
    return query.getResultList();
  }

  public void persist(Userlogins logins) {
    em.persist(logins);
  }
}
