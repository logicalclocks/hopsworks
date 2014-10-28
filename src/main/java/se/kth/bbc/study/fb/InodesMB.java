package se.kth.bbc.study.fb;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.apache.hadoop.fs.Path;
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
import org.primefaces.model.StreamedContent;
import se.kth.bbc.study.StudyMB;
import se.kth.bbc.fileoperations.FileSystemOperations;

/**
 *
 * @author jdowling
 */
@ManagedBean(name = "InodesMB")
@SessionScoped
public class InodesMB implements Serializable {

    Inode root;
    Inode cwd;

    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB study;

    @EJB
    private InodeFacade inodes;

    @EJB
    private FileSystemOperations fsOps;

    public void setStudy(StudyMB study) {
        this.study = study;
    }

    private class BadPath extends Exception {

        public BadPath(String msg) {
            super(msg);
        }
    }

    @PostConstruct
    public void init() {
        //TODO: implement a better way to find inodes that represent root folders
        root = inodes.findByName(study.getStudyName());
        cwd = root;
    }

    public List<Inode> getChildren() {
        //Because InodesMB is session scoped, need to check for change of study!!!!
        //TODO: implement this more gracefully.
        if (!cwd.getStudyRoot().equals(study.getStudyName())) {
            init();
        }
        List<Inode> res = new ArrayList<>();
        res.addAll(inodes.findByParent(cwd));
        if (!cwd.isStudyRoot()) { // root doesn't have a parent to show
            res.add(0, new Inode(0, "..", new Date(), true, cwd.getParent().getStatus()));
        }
        return res;
    }

    public void cdUp() {
        if (!cwd.isStudyRoot()) {
            Inode parent = cwd.getParent();
            // nullify object reference to prevent mem leak
            // TODO: uncomment this line when we get the list of children from the DB.
//            cwd.setParent(null);
            // set cwd to move up a directory
            cwd = parent;
        }
    }

    public void cdDown(String name) {

        for (Inode f : cwd.getChildren()) {
            if (f.getName().compareTo(name) == 0 && f.isDir()) {
                cwd = f;
            }
        }
    }

    private boolean isStudyRoot() {
        return cwd.equals(root);
    }

    /**
     *
     * @param components string for path to parse. Has to be a List supporting
     * remove, so ArrayList here.
     * @param path empty to begin with
     * @param origCwd cwd when calling this method and still cwd when it returns
     * @return list of path components, starting with root.
     */
    private List<Inode> getPathComponents(ArrayList<String> components, List<Inode> path, Inode origCwd)
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

    private int distanceFromRoot(Inode f, int d, Inode origCwd) {
        if (isStudyRoot()) { // base case
            this.cwd = origCwd;
            d++;
            return d;
        }
        d++;
        f = f.getParent();
        return distanceFromRoot(f, d, origCwd);
    }

    /**
     *
     * @param name valid path
     */
    public void cd(String name) {
        System.out.println("DEBUG: path:" + name);
        String[] p = name.split("/");
        ArrayList<String> pathComponents = new ArrayList<>(Arrays.asList(p));
        try {
            List<Inode> path = getPathComponents(pathComponents, new ArrayList<Inode>(), this.cwd);
            // TODO: Do not allow user to change to arbitrary directory outside the project

            // Change cwd to last element in the path
            this.cwd = path.get(path.size() - 1);
        } catch (BadPath ex) {
            Logger.getLogger(InodesMB.class.getName()).log(Level.SEVERE, null, ex);
            // TODO: Faces msg to user here.
        }
    }

    public List<NavigationPath> getCurrentPath() {
        //Because InodesMB is session scoped, need to check for change of study!!!!
        //TODO: implement this more gracefully.
        if (!cwd.getStudyRoot().equals(study.getStudyName())) {
            init();
        }
        return cwd.getConstituentsPath();
    }

    public String getCwdPath() {
        return cwd.getPath();
    }

    public void cdBrowse(String name) {
        String[] p = name.split("/");
        Inode curr = root;
        for (int i = 1; i < p.length; i++) {
            String s = p[i];
            Inode next = curr.getChild(s);
            curr = next;
        }
        cwd = curr;
    }

    public static String approximateTime(Date event) {
        long currentTime = new Date().getTime();
        long fetchedTime = event.getTime();
        if ((currentTime - fetchedTime) / 1000 >= 0 && (currentTime - fetchedTime) / 1000 <= 20) {
            return String.format("less than a minute ago.");
        } else if ((currentTime - fetchedTime) / 1000 > 20 && (currentTime - fetchedTime) / 1000 <= 118) {
            return String.format("about %s minute ago.", 1);
        } else if ((currentTime - fetchedTime) / 1000 > 118 && (currentTime - fetchedTime) / 1000 < 1800) {
            return String.format("%s minutes ago.", (currentTime - fetchedTime) / 60000);
        } else if ((currentTime - fetchedTime) / 1000 > 1800 && (currentTime - fetchedTime) / 1000 <= 7056) {
            return String.format("about %s hour ago.", 1);
        } else if ((currentTime - fetchedTime) / 1000 > 7056 && (currentTime - fetchedTime) / 1000 <= 45400) {
            return String.format("%s hours ago.", (currentTime - fetchedTime) / 3600000);
        } else if ((currentTime - fetchedTime) / 1000 > 45400 && (currentTime - fetchedTime) / 1000 <= 170000) {
            return String.format("about %s day ago.", 1);
        } else if ((currentTime - fetchedTime) / 1000 > 170000 && (currentTime - fetchedTime) / 1000 <= 1300000) {
            return String.format("%s days ago.", (currentTime - fetchedTime) / 86400000);
        } else if ((currentTime - fetchedTime) / 1000 > 1300000 && (currentTime - fetchedTime) / 1000 <= 2500000) {
            return String.format("about %s month ago.", 1);
        } else if ((currentTime - fetchedTime) / 1000 > 2500000 && (currentTime - fetchedTime) / 1000 < 25000000) {
            return String.format("%s months ago.", (currentTime - fetchedTime) / (1000 * 2600000));
        } else {
            return String.format("about %s year ago.", 1);
        }
    }

    public void deleteFile(Inode i) {
        try {
            Path toRm = new Path(i.getPath());
            boolean success = fsOps.rm(toRm);
            if (!success) {
                //TODO: add error message
            } else {
                inodes.remove(i);
            }
        } catch (IOException e) {
            //TODO: add error message.
        }
    }

    public static String getSampleId(String path) {
        String[] p = path.split(File.separator);
        for (int i = 0; i < p.length; i++) {
            if (FileSystemOperations.DIR_SAMPLES.equals(p[i])) {
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
