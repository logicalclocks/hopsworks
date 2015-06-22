package se.kth.bbc.project.fb;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;

/**
 *
 * @author jdowling
 */
@ManagedBean(name = "InodesMB")
@SessionScoped
public class InodesMB implements Serializable {

  private static final Logger logger = Logger.
          getLogger(InodesMB.class.getName());

  private Inode root;
  private Inode cwd;
  private List<Inode> cwdChildren;
  private Inode cwdParent;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @EJB
  private InodeFacade inodes;

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  private class BadPath extends Exception {

    public BadPath(String msg) {
      super(msg);
    }
  }

  @PostConstruct
  public void init() {
    root = inodes.getProjectRoot(sessionState.getActiveProjectname());
    cwd = root;
  }

  public List<InodeView> getChildren() {
    if (!inodes.getProjectNameForInode(cwd).equals(sessionState.
            getActiveProjectname())) {
      init();
    }
    //get from DB and update Inode
    cwdChildren = inodes.findByParent(cwd);
    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      kids.add(new InodeView(i, inodes.getPath(i)));
    }
    if (!inodes.isProjectRoot(cwd)) { // root doesn't have a parent to show
      InodeView parent = InodeView.getParentInode(inodes.getPath(cwd));
      kids.add(0, parent);
    }
    return kids;
  }

  public void cdUp() {
    if (!inodes.isProjectRoot(cwd)) {
      cwd = cwdParent;
      cwdChildren = inodes.getChildren(cwd);
      cwdParent = inodes.findParent(cwd);
    }
  }

  public void cdDown(String name) {
    Inode kid = inodes.findByParentAndName(cwd, name);
    if (kid != null && kid.isDir()) {
      cwdParent = cwd;
      cwd = kid;
      cwdChildren = inodes.getChildren(cwd);
    }
  }

  /**
   *
   * @param components string for path to parse. Has to be a List supporting
   * remove, so ArrayList here.
   * @param path empty to begin with
   * @param origCwd cwd when calling this method and still cwd when it returns
   * @return list of path components, starting with root.
   */
  private List<Inode> getPathComponents(ArrayList<String> components,
          List<Inode> path, Inode origCwd)
          throws BadPath {
    if (components.size() < 1) {
      throw new BadPath("Path was empty");
    }
    if (components.size() == 1) { //base case
      path.add(this.cwd);
      this.cwd = origCwd;
      return Lists.reverse(path); // put the root at the start of the list
    }
    if (path.isEmpty()) {
      this.cwd = this.root;
    }
    path.add(this.cwd);
    cdUp();
    components.remove(0);
    return getPathComponents(components, path, origCwd);
  }

  /**
   *
   * @param name valid path
   */
  public void cd(String name) {
    String[] p = name.split("/");
    ArrayList<String> pathComponents = new ArrayList<>(Arrays.asList(p));
    try {
      List<Inode> path = getPathComponents(pathComponents,
              new ArrayList<Inode>(), this.cwd);
            // TODO: Do not allow user to change to arbitrary directory outside the project

      // Change cwd to last element in the path
      this.cwd = path.get(path.size() - 1);
    } catch (BadPath ex) {
      logger.log(Level.SEVERE, "Tried to cd to an invalid path.", ex);
      // TODO: Faces msg to user here.
    }
  }

  /**
   * Change directory to the global path path. I.e. the path starts with
   * /Projects/... If path does not exist, nothing happens.
   * <p>
   * @param path
   */
  public void cdGlobal(String path) {
    Inode i = inodes.getInodeAtPath(path);
    if (i != null) {
      this.cwd = i;
    }
  }

  public List<NavigationPath> getCurrentPath() {
    if (cwd == null || !inodes.getProjectNameForInode(cwd).equals(sessionState.
            getActiveProjectname())) {
      init();
    }
    return inodes.getConstituentsPath(cwd);
  }

  public String getCwdPath() {
    return inodes.getPath(cwd);
  }

  public void cdBrowse(String name) {
    String[] p = name.split("/");
    Inode curr = root;
    for (int i = 1; i < p.length; i++) {
      String s = p[i];
      Inode next = inodes.findByParentAndName(curr, s);
      curr = next;
    }
    cwd = curr;
  }

  public static String approximateTime(Date event) {
    long currentTime = new Date().getTime();
    long fetchedTime = event.getTime();
    if ((currentTime - fetchedTime) / 1000 >= 0 && (currentTime - fetchedTime)
            / 1000 <= 20) {
      return String.format("less than a minute ago.");
    } else if ((currentTime - fetchedTime) / 1000 > 20 && (currentTime
            - fetchedTime) / 1000 <= 118) {
      return String.format("about %s minute ago.", 1);
    } else if ((currentTime - fetchedTime) / 1000 > 118 && (currentTime
            - fetchedTime) / 1000 < 1800) {
      return String.format("%s minutes ago.", (currentTime - fetchedTime)
              / 60000);
    } else if ((currentTime - fetchedTime) / 1000 > 1800 && (currentTime
            - fetchedTime) / 1000 <= 7056) {
      return String.format("about %s hour ago.", 1);
    } else if ((currentTime - fetchedTime) / 1000 > 7056 && (currentTime
            - fetchedTime) / 1000 <= 45400) {
      return String.format("%s hours ago.", (currentTime - fetchedTime)
              / 3600000);
    } else if ((currentTime - fetchedTime) / 1000 > 45400 && (currentTime
            - fetchedTime) / 1000 <= 170000) {
      return String.format("about %s day ago.", 1);
    } else if ((currentTime - fetchedTime) / 1000 > 170000 && (currentTime
            - fetchedTime) / 1000 <= 1300000) {
      return String.format("%s days ago.", (currentTime - fetchedTime)
              / 86400000);
    } else if ((currentTime - fetchedTime) / 1000 > 1300000 && (currentTime
            - fetchedTime) / 1000 <= 2500000) {
      return String.format("about %s month ago.", 1);
    } else if ((currentTime - fetchedTime) / 1000 > 2500000 && (currentTime
            - fetchedTime) / 1000 < 25000000) {
      return String.format("%s months ago.", (currentTime - fetchedTime) / (1000
              * 2600000));
    } else {
      return String.format("about %s year ago.", 1);
    }
  }

  public static String getSampleId(String path) {
    String[] p = path.split(File.separator);
    for (int i = 0; i < p.length; i++) {
      if (Constants.DIR_SAMPLES.equals(p[i])) {
        if (i + 1 < p.length) {
          return p[i + 1];
        } else {
          return null;
        }
      }
    }
    return null;
  }

}
