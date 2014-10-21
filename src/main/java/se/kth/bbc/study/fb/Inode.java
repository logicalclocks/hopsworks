/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.fb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jdowling
 */
public class Inode implements Serializable {

    public static enum Status {

        UPLOADING, COPYING_TO_HDFS, READY
    };
    boolean dir;
    String name;
    Status status;
    int size;
    List<Inode> children = new ArrayList<>();
    Inode parent;

    public Inode() {
        this("", Status.READY, 0, false, null);
    }

    public Inode(String name, Status status, int size, boolean dir, Inode parent) {
        this.name = name;
        this.status = status;
        this.size = size;
        this.dir = dir;
        this.parent = parent;
    }

    public Inode getParent() {
        return parent;
    }

    public void setParent(Inode parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public int getSize() {
        return size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Inode> getChildren() {
        // TODO - we should get the children from the database
        if (!isDir()) {
            throw new IllegalStateException("Files do not have children, only directories do");
        }
        List<Inode> res = new ArrayList<>();
        res.addAll(this.children);
        if (this.name.compareTo("/") != 0) { // root doesn't have a parent to show
            res.add(new Inode("..", parent.getStatus(),
                    parent.getSize(), true, parent.getParent()));
        }
        return res;
    }

    public void setChildren(List<Inode> children) {
        this.children = children;
    }

    public void addChild(Inode child) {
        this.children.add(child);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj instanceof Inode == false) {
            return false;
        }
        Inode that = (Inode) obj;
        if (this.name.compareTo(that.name) != 0) {
            return false;
        }
        if (that.parent == null && that.parent == null) {
            return true; // root inode
        }
        if (this.parent == null || that.parent == null) {
            return false; // something went wrong here
        }
        if (this.parent.equals(that.parent) != true) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return parent.hashCode() + this.name.hashCode();
    }

    boolean isRoot() {
        return this.name.compareTo("/") == 0 && this.parent == null && this.dir;
    }

    public boolean isDir() {
        return dir;
    }

    public boolean isParent() {
        return name.compareTo("..") == 0;
    }
}
