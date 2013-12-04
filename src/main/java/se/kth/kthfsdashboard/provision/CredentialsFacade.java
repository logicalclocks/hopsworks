/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
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
        return query.getResultList().get(0);
    }
}
