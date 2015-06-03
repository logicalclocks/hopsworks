package se.kth.bbc.project.fb;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.lims.Constants;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class InodeFacade extends AbstractFacade<Inode> {

  @PersistenceContext(unitName = "hopsPU")
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
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find all the Inodes that have <i>parent</i> as parent.
   * <p>
   * @param parent
   * @return
   */
  public List<Inode> findByParent(Inode parent) {
    TypedQuery<Inode> query = em.createNamedQuery("Inode.findByParentId",
            Inode.class);
    query.setParameter("parentId", parent.getId());
    return query.getResultList();
  }

  /**
   * Get all the children of <i>parent</i>. Alias of findByParent().
   * <p>
   * @param parent
   * @return
   */
  public List<Inode> getChildren(Inode parent) {
    return findByParent(parent);
  }

  /**
   * Find the parent of the given Inode. If the Inode has no parent, null is
   * returned.
   * <p>
   * @param i
   * @return The parent, or null if no parent.
   */
  public Inode findParent(Inode i) {
    int id = i.getInodePK().getParentId();
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findById", Inode.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   *
   * @param path
   * @return null if no such Inode found
   */
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
    if (curr == null) {
      return null;
    }
    //Move down the path
    for (int i = 1; i < p.length; i++) {
      Inode next = findByParentAndName(curr, p[i]);
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

  public Inode getProjectRoot(String name) {
    return getInode("/" + Constants.DIR_ROOT + "/" + name);
  }

  public Inode findByParentAndName(Inode parent, String name) {
    TypedQuery<Inode> q = em.createNamedQuery("Inode.findByPrimaryKey",
            Inode.class);
    q.setParameter("inodePk", new InodePK(parent.getId(), name));
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
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

  public boolean isProjectRoot(Inode i) {
    Inode parent = findParent(i);
    if (!parent.getInodePK().getName().equals(
            Constants.DIR_ROOT)) {
      return false;
    } else {
      //A node is the project root if its parent has the name $DIR_ROOT and its grandparent is the root node
      return parent.getInodePK().getParentId() == 1;
    }
  }

  public String getProjectNameForInode(Inode i) {
    Inode projectRoot = getProjectRootForInode(i);
    return projectRoot.getInodePK().getName();
  }

  public List<NavigationPath> getConstituentsPath(Inode i) {
    if (isProjectRoot(i)) {
      List<NavigationPath> p = new ArrayList<>();
      p.add(new NavigationPath(i.getInodePK().getName(), i.getInodePK().
              getName() + "/"));
      return p;
    } else {
      List<NavigationPath> p = getConstituentsPath(findParent(i));
      NavigationPath a;
      if (i.isDir()) {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName() + "/");
      } else {
        a = new NavigationPath(i.getInodePK().getName(), p.get(p.size() - 1).
                getPath() + i.getInodePK().getName());
      }
      p.add(a);
      return p;
    }
  }

  public String getPath(Inode i) {
    List<String> pathComponents = new ArrayList<>();
    Inode parent = i;
    while (parent.getId() != 1) {
      pathComponents.add(parent.getInodePK().getName());
      parent = findParent(parent);
    }
    StringBuilder path = new StringBuilder();
    for (int j = pathComponents.size() - 1; j >= 0; j--) {
      path.append("/").append(pathComponents.get(j));
    }
    return path.toString();
  }

}