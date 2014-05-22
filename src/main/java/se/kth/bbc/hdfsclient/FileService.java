/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


import org.primefaces.model.TreeNode;
import org.primefaces.model.DefaultTreeNode;
/**
 *
 * @author roshan
 */
@ManagedBean(name = "fileService")
@ApplicationScoped
public class FileService {
    
    public final String nameNodeURI = "hdfs://localhost:9999";
    private final String SET_DEFAULT_FS = "fs.defaultFS";
    public static final String DEFAULT_TYPE = "folder";
    
    TreeNode root = null;
    TreeNode researcher = null;
    TreeNode dataSets = null;
    TreeNode datasetName = null;
    TreeNode file = null;
    
    public TreeNode createFiles() throws URISyntaxException, IOException, InterruptedException{
    
        String currentUser = getUsername();
        String rootDir = currentUser.split("@")[0].trim();
        Configuration conf = new Configuration();
        conf.set(SET_DEFAULT_FS, this.nameNodeURI+File.separator+rootDir);
        Path userPath = new Path(conf.get(SET_DEFAULT_FS)); 
        FileSystem fs = FileSystem.get(conf);    
        String parent = userPath.getParent().getName();
        root = new DefaultTreeNode(new TreeFiles(parent, "-" , "Folder"), null);
        
        FileStatus[] files = fs.listStatus(userPath);
        
                  
        for(int i = 0; i<files.length; i++){
              if(files[i].isDirectory()) {
                    if(dataSets == null) {  
                        dataSets = new DefaultTreeNode(new TreeFiles(files[i].getPath().getName(), "-" , "Folder"), root);
                    } else if(datasetName == null && dataSets != null) {
                        datasetName = new DefaultTreeNode(new TreeFiles(files[i].getPath().getName(), "-" , "Folder"), dataSets);
                    } else{
                        return null;
                    }
            } else {
                      file = new DefaultTreeNode(new TreeFiles(files[i].getPath().getName(), "-" , "File"), datasetName);
              }
           }
    
        return root;
    }
    
     private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
}
