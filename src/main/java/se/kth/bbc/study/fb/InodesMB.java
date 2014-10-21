/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.fb;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author jdowling
 */
@ManagedBean(name = "InodesMB")
@SessionScoped
public class InodesMB implements Serializable {

    Inode root = new Inode("/", Inode.Status.READY, 0, true, null);
    Inode cwd;

    private class BadPath extends Exception {

        public BadPath(String msg) {
            super(msg);
        }
    }

    public InodesMB() {
        this.cwd = this.root;
        Inode tmp = new Inode("tmp", Inode.Status.READY, 1024, true, this.cwd);
        this.cwd.addChild(tmp);
        tmp.addChild(new Inode("bbc.txt", Inode.Status.READY, 1024, false, tmp));
    }

    public List<Inode> getChildren() {
                // TODO - we should get the children from the database

        return cwd.getChildren();
    }

    public void cdUp() {
        if (!cwd.isRoot()) {
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

    private boolean isRoot() {
        if (cwd.equals(root)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param components string for path to parse
     * @param path empty to begin with
     * @param origCwd cwd when calling this method and still cwd when it returns
     * @return list of path components, starting with root.
     */
    private List<Inode> getPathComponents(List<String> components, List<Inode> path, Inode origCwd)
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
        components.remove(0);
        return getPathComponents(components, path, origCwd);
    }

    private int distanceFromRoot(Inode f, int d, Inode origCwd) {
        if (isRoot()) { // base case
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
        String[] p = name.split("/");
        List<String> pathComponents = Arrays.asList(p);
        try {
            List<Inode> path = getPathComponents(pathComponents, new ArrayList<Inode>(), this.cwd);
            // TODO: Do not allow user to change to arbitrary directory outside the project
            
            // Change cwd to last element in the path
            this.cwd = path.get(path.size()-1);
        } catch (BadPath ex) {
            Logger.getLogger(InodesMB.class.getName()).log(Level.SEVERE, null, ex);
            // TODO: Faces msg to user here.
        }
    }

}
