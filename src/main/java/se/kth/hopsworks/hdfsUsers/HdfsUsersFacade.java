
package se.kth.hopsworks.hdfsUsers;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;


public class HdfsUsersFacade extends AbstractFacade<HdfsUsers> {
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HdfsUsersFacade() {
    super(HdfsUsers.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
}
