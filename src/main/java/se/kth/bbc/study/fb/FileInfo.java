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
public class FileInfo implements Serializable
{

    public static enum Status { UPLOADING, COPYING_TO_HDFS, READY };
    boolean dir;
    String name;
    String owner;
    Status status;
    int size;
    List<FileInfo> children = new ArrayList<>();
    FileInfo parent;

    public FileInfo() {
        this("", "nobody", Status.READY, 0, false, null);
    }
    
    public FileInfo(String name, String owner, Status status, int size, boolean dir, FileInfo parent) {
        this.name = name;
        this.owner = owner;
        this.status = status;
        this.size = size;
        this.dir = dir;
        if (dir) {
            this.children.add(new FileInfo(".", owner, status, size, false, this));
        }
        this.parent = parent;
    }

    public FileInfo getParent() {
        return parent;
    }

    public void setParent(FileInfo parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public Status getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<FileInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FileInfo> children) {
        this.children = children;
    }

    public void addChild(FileInfo child) {
        this.children.add(child);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj instanceof FileInfo == false) {
            return false;
        }
        FileInfo that = (FileInfo) obj;
        if (this.name.compareTo(that.name)!= 0) {
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
    
}
