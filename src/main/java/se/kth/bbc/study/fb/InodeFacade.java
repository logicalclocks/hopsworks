package se.kth.bbc.study.fb;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.upload.FileSystemOperations;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

    public static final String AVAILABLE = "available";
    public static final String UPLOADING = "uploading";
    public static final String COPYING = "copying_to_hdfs";

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
     * @param path The path to the new file, file itself included.
     * @param dir
     * @param size
     * @param status
     */
    public Inode createAndPersistInode(String path, boolean dir, int size, String status) {
        //TODO: update size and modified date of parent
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] p = path.split("/");
        Inode root = findByName(p[0]);
        Inode curr = root;
        for (int i = 1; i < p.length - 1; i++) {
            String s = p[i];
            Inode next = curr.getChild(s);
            curr = next;
        }
        Inode parent = curr;
        Inode z = new Inode(p[p.length - 1], parent, dir, size, status);
        parent.addChild(z);

        persist(parent);
        persist(z);

        return z;
    }

    public Inode createAndPersistDir(String path, String status) {
        return createAndPersistInode(path, true, 0, status);
    }

    public Inode createAndPersistFile(String path, int size, String status) {
        return createAndPersistInode(path, false, size, status);
    }

    public void persist(Inode i) {
        em.persist(i);
    }

}
