/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
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
    
    TreeNode researcher = null;
    TreeNode dataSets = null;
    TreeNode datasetName = null;
    TreeNode file = null;
    //private List<TreeNode> children;
    
    public TreeNode createFiles() throws URISyntaxException, IOException, InterruptedException{
    
        String rootDir = getUsername().split("@")[0].trim();
        Configuration conf = new Configuration();
        conf.set(SET_DEFAULT_FS, this.nameNodeURI);
        String buildPath = File.separator+rootDir+File.separator+"dataSets";
        FileSystem fs = FileSystem.get(conf);    
        Path path = new Path(buildPath);       

        FileStatus[] files = fs.listStatus(path);
        Path[] paths = FileUtil.stat2Paths(files);
        
        String parent = path.getParent().getName();
        TreeNode root = new DefaultTreeNode(new TreeFiles(parent, "-" , "Folder"), null);
        
        
        
        for(int i = 0; i<paths.length; i++){
                //System.out.println(files[i].getPath().getName());
                
                        datasetName = new DefaultTreeNode(new TreeFiles(paths[i].getName(), "-" , "Folder"), root);
                        if(!datasetName.getChildren().isEmpty()) {
                            for(int j=0;j<datasetName.getChildren().size();j++)
                                file = new DefaultTreeNode("File", datasetName.getChildren().get(j), datasetName);
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
