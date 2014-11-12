package se.kth.bbc.lims;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class EnvironmentVariableFacade extends AbstractFacade<EnvironmentVariable> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EnvironmentVariableFacade() {
        super(EnvironmentVariable.class);
    }

    /**
     * Get the value associated with the given environment variable name.
     *
     * @param name
     * @return Null if name not found.
     */
    public String getValue(String name) {
        TypedQuery<EnvironmentVariable> q = em.createNamedQuery("EnvironmentVariable.findByName", EnvironmentVariable.class);
        try {
            EnvironmentVariable result = q.getSingleResult();
            return result.getValue();
        } catch (NoResultException e) {
            return null;
        }
    }

}
