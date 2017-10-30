package io.hops.hopsworks.common.dao.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnAppResultFacade extends AbstractFacade<YarnAppResult> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppResultFacade() {
    super(YarnAppResult.class);
  }

  @Override
  public List<YarnAppResult> findAll() {
    TypedQuery<YarnAppResult> query = em.createNamedQuery(
            "YarnAppResult.findAll",
            YarnAppResult.class);
    return query.getResultList();
  }

  public List<YarnAppResult> findAllHistory() {
    Query query = em.
            createNativeQuery("SELECT * FROM hopsworks.yarn_app_result",
                    YarnAppResult.class);
    return query.getResultList();
  }

  public YarnAppResult findAllByName(String id) {
    try {
      return em.createNamedQuery("YarnAppResult.findById",
              YarnAppResult.class).setParameter("id", id).
              getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<YarnAppResult> findByUsername(String username) {
    try {
      return em.createNamedQuery("YarnAppResult.findByUsername",
              YarnAppResult.class).setParameter("username", username).
              getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  public YarnAppResult findByStartTime(String startTime) {
    try {
      return em.createNamedQuery("YarnAppResult.findByStartTime",
              YarnAppResult.class).setParameter("startTime", startTime).
              getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public YarnAppResult update(YarnAppResult yarnApp) {
    return em.merge(yarnApp);
  }

}
