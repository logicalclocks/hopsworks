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
@ManagedBean(name = "FilesMB")
@SessionScoped
public class FilesMB implements Serializable {

    List<FileInfo> files = new ArrayList<>();
    FileInfo root = new FileInfo("/", "root", FileInfo.Status.READY, 0, true, null);
    FileInfo cwd;

    private class BadPath extends Exception {

        public BadPath(String msg) {
            super(msg);
        }
    }

    public FilesMB() {
        this.cwd = this.root;
        this.cwd.addChild(new FileInfo("tmp", "jim", FileInfo.Status.READY, 1024, false, this.cwd));
    }

    public List<FileInfo> getFiles() {
        return cwd.getChildren();
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }

    public void cdUp() {
        if (!cwd.isRoot()) {
            // nullify object reference to prevent mem leak
            cwd.setParent(null);
            // set cwd to move up a directory
            cwd = cwd.getParent();
        }
    }

    public void cdDown(String name) {

        for (FileInfo f : cwd.getChildren()) {
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
    private List<FileInfo> getPathComponents(List<String> components, List<FileInfo> path, FileInfo origCwd)
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

    private int distanceFromRoot(FileInfo f, int d, FileInfo origCwd) {
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
            List<FileInfo> path = getPathComponents(pathComponents, new ArrayList<FileInfo>(), this.cwd);
            // TODO: Do not allow user to change to arbitrary directory outside the project
            
            // Change cwd to last element in the path
            this.cwd = path.get(path.size()-1);
        } catch (BadPath ex) {
            Logger.getLogger(FilesMB.class.getName()).log(Level.SEVERE, null, ex);
            // TODO: Faces msg to user here.
        }
    }

}
