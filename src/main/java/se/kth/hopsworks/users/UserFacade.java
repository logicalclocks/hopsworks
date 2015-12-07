package se.kth.hopsworks.users;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class UserFacade extends AbstractFacade<Users> {



  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public UserFacade() {
    super(Users.class);
  }

  @Override
  public List<Users> findAll() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAll",
            Users.class);
    return query.getResultList();
  }

  public List<Users> findAllByName() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAllByName",
            Users.class);
    return query.getResultList();
  }

  public List<Users> findAllUsers() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.users",
            Users.class);
    return query.getResultList();
  }

  public Users findByUsername(String username) {
    try {
      return em.createNamedQuery("Users.findByUsername", Users.class).setParameter(
              "username", username)
              .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  public List<Users> filterUsersBasedOnProject(String name) {

    Query query = em.createNativeQuery(
            "SELECT * FROM hopsworks.users WHERE email NOT IN (SELECT team_member FROM hopsworks.ProjectTeam WHERE name=?)",
            Users.class).setParameter(1, name);
    return query.getResultList();
  }

  public void persist(Users user) {
    em.persist(user);
  }

  public int lastUserID() {

    Query query = em.createNativeQuery(
            "SELECT MAX(p.uid) FROM hopsworks.users p");
    Object obj = query.getSingleResult();

    if (obj == null) {
      return AuthenticationConstants.STARTING_USER;
    }
    return (Integer) obj;
  }

  public void update(Users user) {
    em.merge(user);
  }

  public void removeByEmail(String email) {
    Users user = findByEmail(email);
    if (user != null) {
      em.remove(user);
    }
  }

  @Override
  public void remove(Users user) {
    if (user != null && user.getEmail() != null && em.contains(user)) {
      em.remove(user);
    }
  }

  /**
   * Get the user with the given email.
   * <p/>
   * @param email
   * @return The user with given email, or null if no such user exists.
   */
  public Users findByEmail(String email) {
    try {
      return em.createNamedQuery("Users.findByEmail", Users.class).setParameter(
              "email", email)
              .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void detach(Users user) {
    em.detach(user);
  }

  /**
   * Get all users with STATUS = status.
   *
   * @param status
   * @return
   */
  public List<Users> findAllByStatus(int status) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatus",
            Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

}
