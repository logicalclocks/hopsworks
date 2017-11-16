package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class RolesAuditFacade extends AbstractFacade<RolesAudit> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public RolesAuditFacade() {
    super(RolesAudit.class);
  }

  public List<RolesAudit> findByInitiator(Users user) {
    TypedQuery<RolesAudit> query = em.createNamedQuery("RolesAudit.findByInitiator", RolesAudit.class);
    query.setParameter("initiator", user);

    return query.getResultList();
  }

  public List<RolesAudit> findByTarget(Users user) {
    TypedQuery<RolesAudit> query = em.createNamedQuery("RolesAudit.findByTarget", RolesAudit.class);
    query.setParameter("target", user);

    return query.getResultList();
  }

}
