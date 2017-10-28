package io.hops.hopsworks.common.dao.project.payment;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class LastPaymentFacade extends
    AbstractFacade<LastPaymentFacade> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public LastPaymentFacade() {
    super(LastPaymentFacade.class);
  }

  public LastPayment findByProjectName(String projectname) {
    TypedQuery<LastPayment> query = em.
        createNamedQuery("LastPayment.findByProjectname",
            LastPayment.class).setParameter("projectname",
            projectname);
    try {
      List<LastPayment> res = query.getResultList();
      if(res==null){
        return null;
      }
      return res.get(0);
    } catch (NoResultException e) {
      return null;
    }
  }

}
