
package se.kth.rest.application.config;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;


@Stateless
public class VariablesFacade extends AbstractFacade<Variables> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public VariablesFacade() {
    super(Variables.class);
  }

  /**
   * Get the variable value with the given name.
   * <p/>
   * @param id
   * @return The user with given email, or null if no such user exists.
   */
  public Variables findById(String id) {
    try {
      return em.createNamedQuery("Variables.findById", Variables.class).setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void detach(Variables variable) {
    em.detach(variable);
  }

}
