package se.kth.bbc.activity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author roshan
 */
@Stateless
public class UserGroupsController {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public UserGroupsController() {
  }

  public void persistUserGroups(UsersGroups userGroups) {
    em.persist(userGroups);
  }

  public void removeUserGroups(String email, String groupname) {
    UsersGroups ug = findByPrimaryKey(email, groupname);
    if (ug != null) {
      em.remove(ug);
    }
  }

  public UsersGroups findByPrimaryKey(String email, String groupname) {
    return em.find(UsersGroups.class, new UsersGroups(new UsersGroupsPK(email,
            groupname)).getUsersGroupsPK());
  }

  /**
   * Deletes only GUEST role of a project from USERS_GROUPS table
   * <p>
   * @param email
   */
  public void clearGroups(String email) {
    em.createNamedQuery("UsersGroups.deleteGuestForEmail", UsersGroups.class).
            setParameter("email", email).executeUpdate();
  }

  /**
   * Check if an entry exists in the UserGroups table for the given email.
   * <p>
   * @param email
   * @return True if one or more entries exists, false otherwise.
   */
  public boolean existsEntryForEmail(String email) {
    Query query = em.createNamedQuery("UsersGroups.findByEmail",
            UsersGroups.class).setParameter("email", email);
    try {
      query.getSingleResult();
      return true;
    } catch (NoResultException e) {
      return false;
    } catch (NonUniqueResultException e) {
      return true;
    }
  }

}
