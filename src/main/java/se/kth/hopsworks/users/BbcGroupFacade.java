package se.kth.hopsworks.users;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
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
    List<BbcGroup> allGroups = em.createNamedQuery("BbcGroup.findAll", BbcGroup.class).getResultList();
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
