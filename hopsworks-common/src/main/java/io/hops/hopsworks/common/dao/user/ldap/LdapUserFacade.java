package io.hops.hopsworks.common.dao.user.ldap;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Stateless
public class LdapUserFacade extends AbstractFacade<LdapUser> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public LdapUserFacade() {
    super(LdapUser.class);
  }
  
  public LdapUser findByLdapUid(String entryUuid) {
    return em.find(LdapUser.class, entryUuid);
  }

  public LdapUser findByUsers(Users user) {
    try {
      return em.createNamedQuery("LdapUser.findByUid", LdapUser.class).setParameter("uid", user).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
