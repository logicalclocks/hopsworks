/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

import java.io.Serializable;

/**
 *
 * @author roshan
 */
public class TreeFiles implements Serializable, Comparable<TreeFiles> {
    
    private String name;
     
    private String size;
     
    private String owner;
     
    public TreeFiles(String name, String size, String owner) {
        this.name = name;
        this.size = size;
        this.owner = owner;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public String getSize() {
        return size;
    }
 
    public void setSize(String size) {
        this.size = size;
    }
 
    public String getType() {
        return owner;
    }
 
    public void setType(String owner) {
        this.owner = owner;
    }
 
    //Eclipse Generated hashCode and equals
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        return result;
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TreeFiles other = (TreeFiles) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        return true;
    }
 
    @Override
    public String toString() {
        return name;
    }
 
    @Override
    public int compareTo(TreeFiles file) {
        return this.getName().compareTo(file.getName());
    }
    
    
}
