package se.kth.hopsworks.hdfsUsers;

import javax.ejb.Stateless;
import se.kth.hopsworks.hdfsUsers.model.HdfsUsers;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;
@Stateless
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

  public HdfsUsers findHdfsUser(byte[] id) {
    return em.find(HdfsUsers.class, id);
  }

  public HdfsUsers findByName(String name) {
    try {
      return em.createNamedQuery("HdfsUsers.findByName", HdfsUsers.class).
              setParameter(
                      "name", name).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persist(HdfsUsers user) {
    em.persist(user);
  }

  public void merge(HdfsUsers user) {
    em.merge(user);
  }

  public void removeHdfsUser(HdfsUsers user) {
    HdfsUsers u = em.find(HdfsUsers.class, user.getId());
    if (u != null) {
      em.createNamedQuery("HdfsUsers.delete", HdfsUsers.class).
              setParameter("id", user.getId()).executeUpdate();
    }
  }
}
