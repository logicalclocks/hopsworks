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

    public Inode findByName(String name) {
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByName", Inode.class);
        query.setParameter("name", name);
        return query.getSingleResult();
    }

    public List<Inode> findByParent(Inode parent) {
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParent", Inode.class);
        query.setParameter("parent", parent);
        return query.getResultList();
    }

    /**
     *
     * @param name
     * @param dir
     * @param size
     * @param status
     * @param path The path to the new file, file itself included.
     */
    public Inode createInode(String name, boolean dir, int size, String status, String path) {
        path = path.substring(path.indexOf("Projects")+9);
        String[] p = path.split("/");
        Inode root = findByName(p[0]);
        Inode curr = root;
        for (int i = 1; i < p.length-1; i++) {
            String s = p[i];
            Inode next = curr.getChild(s);
            curr = next;
        }
        Inode parent = curr;
        Inode z = new Inode(name, parent, dir, size, status);
        parent.addChild(z);
        return z;
    }
    
    public void persist(Inode i){
        em.persist(i);
    }

}
