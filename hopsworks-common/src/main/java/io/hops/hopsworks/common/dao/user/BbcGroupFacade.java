package io.hops.hopsworks.common.dao.user;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class BbcGroupFacade extends AbstractFacade<BbcGroup> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public BbcGroupFacade() {
    super(BbcGroup.class);
  }

  public BbcGroup findByGroupName(String name) {
    try {
      return em.createNamedQuery("BbcGroup.findByGroupName", BbcGroup.class)
              .setParameter("groupName", name).getSingleResult();
    } catch (javax.persistence.NoResultException e) {
      return null;
    }
  }

  public List<BbcGroup> findAll() {
    List<BbcGroup> allGroups = em.createNamedQuery("BbcGroup.findAll",
            BbcGroup.class).getResultList();
    List<BbcGroup> updated = new ArrayList<>();
    if (allGroups != null) {
      for (BbcGroup g : allGroups) {
        if (g.getGroupName().compareTo("AGENT") != 0) {
          updated.add(g);
        }
      }
    }
    return updated;
  }

}
