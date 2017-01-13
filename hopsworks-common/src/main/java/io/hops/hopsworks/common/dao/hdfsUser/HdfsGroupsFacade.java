package io.hops.hopsworks.common.dao.hdfsUser;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class HdfsGroupsFacade extends AbstractFacade<HdfsGroups> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HdfsGroupsFacade() {
    super(HdfsGroups.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsGroups findHdfsGroup(byte[] id) {
    return em.find(HdfsGroups.class, id);
  }

  public HdfsGroups findByName(String name) {
    try {
      return em.createNamedQuery("HdfsGroups.findByName", HdfsGroups.class).
              setParameter(
                      "name", name).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persist(HdfsGroups user) {
    em.persist(user);
  }

  public void merge(HdfsGroups user) {
    em.merge(user);
    em.flush();
  }

  @Override
  public void remove(HdfsGroups group) {
    HdfsGroups g = em.find(HdfsGroups.class, group.getId());
    if (g != null) {
      em.createNamedQuery("HdfsGroups.delete", HdfsGroups.class).
              setParameter("id", group.getId()).executeUpdate();
    }
  }
}
