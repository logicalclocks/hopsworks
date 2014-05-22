/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

import org.primefaces.model.TreeNode;
import org.primefaces.model.DefaultTreeNode;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.viewfs.ViewFileSystem;

/**
 *
 * @author roshan
 */

@ManagedBean(name = "hdfsClientView")
@SessionScoped
public class HdfsClient implements Serializable{
    
//    public final String nameNodeURI = "hdfs://localhost:9999";
//    private final String SET_DEFAULT_FS = "fs.defaultFS";
//    public static final String DEFAULT_TYPE = "folder";
    
     
    private TreeNode root;
    private TreeNode selectedNode;
    
    @ManagedProperty("#{fileService}")
    private FileService service;
    
    @PostConstruct
    public void init() throws URISyntaxException, IOException, InterruptedException{
        root = service.createFiles();
    }
    
//    public void fetchFiles() throws URISyntaxException, IOException, InterruptedException {
//        
//        String currentUser = getUsername();
//        Configuration conf = new Configuration();
//        conf.set(SET_DEFAULT_FS, this.nameNodeURI+File.separator+currentUser);
//        Path userPath = new Path(conf.get(SET_DEFAULT_FS)); 
//        FileSystem fs = FileSystem.get(conf);    
//        
//        FileStatus[] files = fs.listStatus(userPath);
//        int i = 0;    
//        for(FileStatus file: files){
//               if(i < files.length) {
////                    treeNodes[i] = new DefaultTreeNode(DEFAULT_TYPE, file, root);
////                    root = treeNodes[i];
////                    i++;
//               }
//            }
//    }
    
    public TreeNode getRoot() {
        return root;
    }
 
    public void setRoot(TreeNode root) {
        this.root = root;
    }
 
    public TreeNode getSelectedNode() {
        return selectedNode;
    }
 
    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }
      
    public void setService(FileService service) {
        this.service = service;
    }
    
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
    
    
    
    
}
