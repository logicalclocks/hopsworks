package io.hops.hopsworks.common.dao.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnAppHeuristicResultDetailsFacade extends AbstractFacade<YarnAppHeuristicResultDetails> {

  private static final Logger logger = Logger.getLogger(
          YarnAppHeuristicResultDetailsFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppHeuristicResultDetailsFacade() {
    super(YarnAppHeuristicResultDetails.class);
  }

  public String searchByIdAndName(int yarnAppHeuristicResultId, String name) {

    try {
      TypedQuery<YarnAppHeuristicResultDetails> q = em.createNamedQuery(
              "YarnAppHeuristicResultDetails.findByIdAndName",
              YarnAppHeuristicResultDetails.class);
      q.setParameter("yarnAppHeuristicResultId", yarnAppHeuristicResultId);
      q.setParameter("name", name);

      YarnAppHeuristicResultDetails result = q.getSingleResult();
      return result.getValue();

    } catch (NoResultException e) {
      return "UNDEFINED";
    }
  }
}
