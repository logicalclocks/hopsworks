package se.kth.kthfsdashboard.provision;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * Entry point to the data source to query the persisted PaaS Credentials
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Stateless
public class CredentialsFacade extends AbstractFacade<PaaSCredentials>{
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    
    @Override
    protected EntityManager getEntityManager() {
       return em;
    }
    
    public CredentialsFacade(){
        super(PaaSCredentials.class);
    }
    
    public PaaSCredentials find() {
        TypedQuery<PaaSCredentials> query = em.createNamedQuery("PaaSCredentials.findAll", PaaSCredentials.class);
        if (query.getResultList().size() > 0) {
            return query.getResultList().get(0);
        }
        return new PaaSCredentials();
    }
}
