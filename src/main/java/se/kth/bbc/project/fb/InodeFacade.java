package se.kth.bbc.project.fb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.fileoperations.FileOperations;
import static se.kth.bbc.fileoperations.Operation.ADD;
import se.kth.bbc.lims.Constants;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.Templates;

/**
 *
 * @author stig
 */
@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

    private static final Logger logger = Logger.getLogger(InodeFacade.class.
            getName());

    @EJB
    InodeOpsFacade inodeOps;

    @EJB
    private FileOperations fileOps;

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
        if (name == null) {
            return null;
        }

        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByName",
                Inode.class);
        query.setParameter("name", name);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Inode> findByParent(Inode parent) {
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParent",
                Inode.class);
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
    private Inode createAndPersistInode(String path, int rootStudy, boolean dir,
            long size, String status, int templateId) {
        //TODO: update size and modified date of parent
        //TODO: make all occurences of 'size' in application longs.
        Inode parent = getLastCreatedNodeOnPath(path);

        Templates template = new Templates(templateId);

        String[] p = path.split("/");
        Inode z = new Inode(p[p.length - 1], parent, rootStudy, dir, false,
                (int) size, status);
        z.addTemplate(template);
        parent.addChild(z);

        em.persist(parent);
        em.persist(z);

        return z;
    }

    private Inode createAndPersistInode(Inode parent, int rootstudy, String name,
            boolean dir, long size, String status, int templateId) {

        Templates template = new Templates(templateId);
        if (template.getId() == -1) {
            //let the gc remove the garbage
            template = null;
        }

        Inode z;
        if (parent != null) {
            z = new Inode(name, parent, rootstudy, dir, false, (int) size, status);
            z.addTemplate(template);

            parent.addChild(z);
            em.persist(parent);
        } else {

            z = new Inode(name, parent, rootstudy, dir, false, (int) size, status);
            z.addTemplate(template);
        }

        em.persist(z);
        return z;
    }

    /**
     * Creates the inode-dir (and its parent dirs) if it doesn't exist or
     * returns the existing inode dir. Equivalent of: mkdir -p
     * <p>
     * @param path
     * @param status
     * @param templateId
     * @return the Inode for the directory (whether it already exists or has
     * just been created)
     */
    public Inode createAndPersistDir(String path, String status, int templateId) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] p = path.split("/");
        Inode root = findByName(p[0]);
        if (root == null) {
            root = createAndPersistInode(null, 0, p[0], true, 0, status, templateId);
            this.inodeOps.createAndStoreOperation(root, ADD);
        }

        int mainRoot = root.getId();

        //find the main study node - the study root
        Inode mainStudy = findByName(p[1]);
        int studyRoot = (mainStudy == null) ? 0 : mainStudy.getId();

        Inode curr = root;
        for (int i = 1; i < p.length; i++) {
            String s = p[i];
            Inode next = curr.getChild(s);

            if (next == null) {
                if (i == 1) {
                    next = createAndPersistInode(curr, mainRoot, s, true, 0, status, templateId);
                    this.update(next);
                    System.err.println("CREATING DIR " + next.getName());
                    this.inodeOps.createAndStoreOperation(next, ADD);
                } else {
                    next = createAndPersistInode(curr, studyRoot, s, true, 0, status, templateId);
                    this.update(next);
                    System.err.println("UPDATING DIR " + next.getName());
                    this.inodeOps.createAndStoreOperation(next, ADD);
                }
            }
            curr = next;
        }
        return curr;
    }

    public Inode createAndPersistFile(String path, long size, String status, int templateId) {
        //leave out any garbage in the path
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        //get the study name
        String rootStudy = path.split("/")[1];
        //get the study node
        Inode studyNode = findByName(rootStudy);

        return createAndPersistInode(path, studyNode.getId(), false, size, status, templateId);
    }

    public void persist(Inode i) {
        em.persist(i);
        em.flush();
    }

    public void update(Inode i) {
        em.merge(i);
        em.flush();
    }

    /**
     * Remove the inode on path <i>location</i> from the DB.
     * <p>
     * @param location The location of the file or folder to remove recursively.
     * @return True if the operation succeeded, false if it failed. A return
     * value of false should be treated with caution as this may result in
     * inconsistencies between the file system and its representation in the DB.
     */
    public boolean removeRecursivePath(String location) {
        Inode toRem = getInode(location);
        if (toRem == null) {
            return false;
        }

        try {
            this.fileOps.rmR(toRem);
            //em.remove(toRem);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param path
     * @return null if no such Inode found
     */
    public Inode getInode(String path) {
        // Get the path components
        String[] p;
        if (path.charAt(0) == '/') {
            p = path.substring(1).split("/");
        } else {
            p = path.split("/");
        }

        if (p.length < 1) {
            return null;
        }

        //Get the right root node
        Inode curr = getRootNode(p[0]);
        if (curr == null) {
            return null;
        }
        //Move down the path
        for (int i = 1; i < p.length; i++) {
            Inode next = curr.getChild(p[i]);
            if (next == null) {
                return null;
            } else {
                curr = next;
            }
        }
        return curr;
    }

    private Inode getRootNode(String name) {
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findRootByName",
                Inode.class);
        query.setParameter("name", name);
        try {
            return query.getSingleResult(); //Sure to give a single result because all children of same parent "null" so name is unique
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Check whether the given path exists.
     * <p>
     * @param path The path to search for.
     * @return True if the path exist (i.e. there is an Inode on this path),
     * false otherwise.
     */
    public boolean existsPath(String path) {
        return getInode(path) != null;
    }

    /**
     * Get the Inode at the specified path.
     * <p>
     * @param path
     * @return Null if path does not exist.
     */
    public Inode getInodeAtPath(String path) {
        return getInode(path);
    }

    private Inode getLastCreatedNodeOnPath(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] p = path.split("/");
        Inode root = findByName(p[0]);
        Inode curr = root;
        for (int i = 1; i < p.length - 1; i++) {
            String s = p[i];
            Inode next = curr.getChild(s);
            if (next != null) {
                curr = next;
            } else {
                break;
            }
        }
        return curr;
    }

    public Inode getProjectRoot(String name) {
        return getInode("/" + Constants.DIR_ROOT + "/" + name);
    }

    public Inode findParent(Inode i) {
        int id = i.getParent().getId();
        TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
        q.setParameter("id", id);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public String getPath(Inode i) {
        List<String> pathComponents = new ArrayList<>();
        Inode parent = i;
        while (parent.getId() != 1) {
            pathComponents.add(parent.getName());
            parent = findParent(parent);
        }
        StringBuilder path = new StringBuilder();
        for (int j = pathComponents.size() - 1; j >= 0; j--) {
            path.append("/").append(pathComponents.get(j));
        }
        return path.toString();
    }

    public boolean isProjectRoot(Inode i) {
        Inode parent = findParent(i);
        if (!parent.getName().equals(
                Constants.DIR_ROOT)) {
            return false;
        } else {
            //A node is the project root if its parent has the name $DIR_ROOT and its grandparent is the root node
            return parent.getParent().getId() == 1;
        }
    }

    public Inode getProjectRootForInode(Inode i) {
        if (isProjectRoot(i)) {
            return i;
        } else {
            Inode parent = findParent(i);
            if (parent == null) {
                throw new IllegalStateException(
                        "Transversing the path from folder did not encounter project root folder.");
            }
            return getProjectRootForInode(parent);
        }
    }

    public Inode findByParentAndName(Inode parent, String name) {
        TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParentAndName",
                Inode.class);
        query.setParameter("parent", parent);
        query.setParameter("name", name);
        try {
            return query.getSingleResult(); //Sure to give a single result because all children of same parent "null" so name is unique
        } catch (NoResultException e) {
            return null;
        }
    }
}
