/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author roshan
 */
public class TreeFiles implements Serializable{
    
    private String type;
    private Object file;
    
    private TreeFiles rootDir;
    private List<TreeFiles> subDirs;
    private boolean expanded;
    
    @SuppressWarnings("LeakingThisInConstructor")
    public TreeFiles(Object file, TreeFiles rootDir){
        
        this.type = "Folder";
        this.file = file;
        subDirs = new ArrayList<TreeFiles>();
        this.rootDir = rootDir;
        if(this.rootDir != null){
           this.rootDir.getSubdirs().add(this);
        }
    }
    
    
    @SuppressWarnings("LeakingThisInConstructor")
    public TreeFiles(String type, Object file, TreeFiles rootDir){
        
        this.type = type;
        this.file = file;
        subDirs = new ArrayList<TreeFiles>();
        this.rootDir = rootDir;
        if(this.rootDir != null){
           this.rootDir.getSubdirs().add(this);
        }
    }
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public Object getFile(){
        return file;
    }
    
    public void setFile(Object file){
        this.file = file;
    }
    
    public List<TreeFiles> getSubdirs(){
        return subDirs;
    }
    
    public void setSubdirs(List<TreeFiles> subDirs){
        this.subDirs = subDirs;
    }
    
    public TreeFiles getParent() {
                return rootDir;
    }
        
    public void setParent(TreeFiles rootDir) {
         this.rootDir = rootDir;
    }
    
    public boolean isExpanded(){
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
                this.expanded = expanded;
                
                if(rootDir != null) {
                        rootDir.setExpanded(expanded);
                }
    }
}
