package se.kth.bbc.study.fb;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public InodeFacade() {
        super(Inode.class);
    }
    
    public Inode findByName(String name){
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByName", Inode.class);
        query.setParameter("name", name);
        return query.getSingleResult();
    }
    
    public List<Inode> findByParent(Inode parent){
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParent",Inode.class);
        query.setParameter("parent", parent);
        return query.getResultList();
    }

}
