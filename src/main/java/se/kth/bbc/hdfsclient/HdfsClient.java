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
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.viewfs.ViewFileSystem;

/**
 *
 * @author roshan
 */

@ManagedBean
@RequestScoped
public class HdfsClient implements Serializable{
    
    public final String nameNodeURI = "hdfs://localhost:9999";
    private final String SET_DEFAULT_FS = "fs.defaultFS";
    public static final String DEFAULT_TYPE = "folder";
    
    private TreeFiles root;
    private TreeFiles[] treeFile;
    private TreeFiles selectedFile;
    
    
    //private List<TreeFiles> dirTree;
    
    
    public void fetchFiles() throws URISyntaxException, IOException, InterruptedException {
        
        root = new TreeFiles("Root", null);
        
        String currentUser = getUsername();
        Configuration conf = new Configuration();
        conf.set(SET_DEFAULT_FS, this.nameNodeURI+File.separator+currentUser);
        Path userPath = new Path(conf.get(SET_DEFAULT_FS)); 
        FileSystem fs = FileSystem.get(conf);    
        
        FileStatus[] files = fs.listStatus(userPath);
        int i = 0;    
        for(FileStatus file: files){
               if(i < files.length) {
                    treeFile[i] = new TreeFiles(DEFAULT_TYPE, file, root);
                    root = treeFile[i];
                    i++;
               }
            }
    }
    
    public TreeFiles getRoot(){
        return root;
    }
    
    public void setRoot(TreeFiles root){
        this.root = root;
    }
    
    
    public TreeFiles getSelectedFile(){
        return selectedFile;
    }
    
    public void setSelectedFile(TreeFiles selectedFile){
        this.selectedFile = selectedFile;
    }
    
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
    
    
    
    
}
