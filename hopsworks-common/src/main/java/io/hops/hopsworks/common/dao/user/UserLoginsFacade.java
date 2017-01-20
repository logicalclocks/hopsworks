package io.hops.hopsworks.common.dao.user;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.audit.Userlogins;

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
