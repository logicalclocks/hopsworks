package io.hops.hopsworks.common.dao.user.consent;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class ConsentsFacade extends AbstractFacade<Consents> {

  private static final Logger logger = Logger.getLogger(ConsentsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ConsentsFacade() {
    super(Consents.class);
  }

  public List<Consents> findAllInProject(int projectId) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByProjectId",
            Consents.class);
    q.setParameter("id", projectId);
    return q.getResultList();
  }

  public void persistConsent(Consents consent) {
    em.persist(consent);
  }

}
