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
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByName",
            Inode.class);
    query.setParameter("name", name);
    return query.getSingleResult();
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
  private Inode createAndPersistInode(String path, boolean dir, long size,
          String status) {
        //TODO: update size and modified date of parent
    //TODO: make all occurences of 'size' in application longs.
    Inode parent = getLastCreatedNodeOnPath(path);
    String[] p = path.split("/");
    Inode z = new Inode(p[p.length - 1], parent, dir, (int) size, status);
    parent.addChild(z);

    em.persist(parent);
    em.persist(z);

    return z;
  }
  
  private Inode createAndPersistInode(Inode parent, String name, boolean dir, long size, String status){
    Inode z = new Inode(name, parent, dir, (int) size, status);
    parent.addChild(z);

    em.persist(parent);
    em.persist(z);

    return z;
  }

  /**
   * Creates the inode-dir (and its parent dirs) if it doesn't exist or returns the existing inode dir.
   * Equivalent of: mkdir -p
   * @param path
   * @param status
   * @return the Inode for the directory (whether it already exists or has just been created)
   */
  public Inode createAndPersistDir(String path, String status) {
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] p = path.split("/");
    Inode root = findByName(p[0]);
    Inode curr = root;
    for (int i = 1; i < p.length; i++) {
      String s = p[i];
      Inode next = curr.getChild(s);
      if (next == null) {
        next = createAndPersistInode(curr, s, true, 0, status);
      }
      curr = next;
    }
    return curr;
  }

  public Inode createAndPersistFile(String path, long size, String status) {
    return createAndPersistInode(path, false, size, status);
  }

  public void persist(Inode i) {
    em.persist(i);
  }

  public void update(Inode i) {
    em.merge(i);
  }

  /**
   * Remove the inode on path <i>location</i> from the DB.
   * <p>
   * @param location The location of the file or folder to remove recursively.
   * @return True if the operation succeeded, false if it failed. A return value
   * of false should be treated with caution as this may result in
   * inconsistencies between the file system and its representation in the DB.
   */
  public boolean removeRecursivePath(String location) {
    Inode toRem = getInode(location);
    if (toRem == null) {
      return false;
    }
    em.remove(toRem);
    return true;
  }

  private Inode getInode(String path) {
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
    return query.getSingleResult(); //Sure to give a single result because both all children of same parent "null" so name is unique
    //TODO: enforce uniqueness of folder and file name under same parent in Inodes table!
  }

  /**
   * Check whether the given path exists.
   * <p>
   * @param path The path to search for.
   * @return True if the path exist (i.e. there is an Inode on this path), false
   * otherwise.
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

}
