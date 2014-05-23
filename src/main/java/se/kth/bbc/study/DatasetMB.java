/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author roshan
 */
@ManagedBean
@SessionScoped
public class DatasetMB implements Serializable{
    
    @EJB
    private DatasetController datasetController;
    private Dataset dataset;
    
    private String owner;
    private String datasetName;
    
    public final String nameNodeURI = "hdfs://localhost:9999";
    private StreamedContent file;
    
//    @ManagedProperty("#{param['formId:dataset_name']}")
//    private String dataset_name;
    
    
    
    public String getOwner(){
        return owner;
    }
    
    public void setOwner(String owner){
        this.owner = owner;
    }
    
    public String getDatasetName(){
        return datasetName;
    }
    
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    
    public Dataset getDataset() {
        if (dataset == null) {
            dataset = new Dataset();
        }
        return dataset;
    }
    
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    } 
        
    public String getDatasetOwner(){
        return getUsername();
    }
    
    
    public String createDataset()throws IOException, URISyntaxException{
        
             
        dataset.setId(Integer.SIZE);     
        dataset.setOwner(getUsername());
      
        try{
            datasetController.persistDataset(dataset);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Dataset wasn't created. Dataset name might have been duplicated!");
            return null;
        }
        addMessage("Dataset created! ["+ dataset.getName() + "] dataset is owned by " + dataset.getOwner());
        mkDIRS(dataset.getOwner(),dataset.getName());
        return "dataUpload";
    }
    
    public String deleteDataset(){
        try{
            datasetController.removeDataset(dataset);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Dataset wasn't removed.");
            return null;
        }
        addMessage("Dataset removed.");
        return "Success!";
    }
   
    
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
   
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
       

    //Creating directory structure in HDFS
    public void mkDIRS(String dsOwner, String dsName) throws IOException, URISyntaxException{
    
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        String rootDir = dsOwner.split("@")[0].trim();
        
        String buildPath = File.separator+rootDir+File.separator+"dataSets";
        FileSystem fs = FileSystem.get(conf);
        Path path = new Path(buildPath);
               
        try {
            if (fs.exists(path.getParent())) {
                Path.mergePaths(path, new Path(File.separator+dsName));
                addMessage("Dataset directory created!" + "/"+dsName);
            }
                fs.mkdirs(path.suffix(File.separator+dsName), null);
                addMessage("Dataset directory created!" + buildPath);
//                FileStatus[] files = fs.listStatus(path);
//                    for(FileStatus file: files){
//                        System.out.println(file.getPath().getName());
//                }
            
         } catch(IOException ioe){
            System.err.println("IOException during operation"+ ioe.toString());
            System.exit(1);
         }finally {
                fs.close();
        }
        
    }

    
    //Staging 
    
    public void stagingToGlassfish(InputStream is, String filename){
    
        try{
            //URL uri = new URL("http://snurran.sics.se/hop");
            OutputStream os = new FileOutputStream(new File("/home/glassfish/roshan/samples/staging"+File.separator+filename));
            
            byte[] buffer = new byte[10248576]; 
            int readBytes = 0;
         
            while((readBytes=is.read(buffer)) > 0){
                os.write(buffer,0,readBytes);
            }
                os.close();
                is.close();
                //addMessage("File staging completed... and is transferring to hdfs...."+filename);
        } catch(FileNotFoundException fnf){
            addErrorMessageToUserAction("File not found! "+ fnf.toString());    
        } catch(IOException ioe){
            addErrorMessageToUserAction("I/O Exception "+ ioe.toString());    
        }
        
    }
    
    
    public void copyFromLocal(String filename) throws IOException, URISyntaxException {
    
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        
        String rootDir = getUsername().split("@")[0].trim();
        String buildPath = File.separator+rootDir+File.separator+"dataSets";
        
        Path outputPath = new Path(buildPath+File.separator+dataset.getName()+File.separator+filename);
        
        if(fs.exists(outputPath)){
            addErrorMessageToUserAction("Error: "+filename+" File exists!");
            return;
        } 
    
        InputStream is = new FileInputStream(new File("/home/glassfish/roshan/samples/staging"+File.separator+filename));
        FSDataOutputStream os = fs.create(outputPath, false);
        IOUtils.copyBytes(is, os, 10248576, true);
        addMessage("File copied to hdfs  :"+ outputPath);
    }
    
    
    
    //Streaming data through dashboard to HDFS
    public void fileUploadEvent(FileUploadEvent event) throws IOException, URISyntaxException{
        
        InputStream is = event.getFile().getInputstream();
        stagingToGlassfish(is, event.getFile().getFileName());
        copyFromLocal(event.getFile().getFileName());

    }
    
    
    //Download file from HDFS
    public void fileDownloadEvent() throws IOException, URISyntaxException{
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        Path outputPath = new Path("/roshan/dataSets/Five/dmd-tsit.avi");
        String fileName = outputPath.getName();
        
        try {
        
                if(!fs.exists(outputPath)){
                      addErrorMessageToUserAction("Error: File does not exist!" + fileName);
                      return;
                }
           
                   InputStream inStream = fs.open(outputPath, 10248576);    
                   file = new DefaultStreamedContent(inStream, "VCF/BAM/ADAM", fileName);
                   
        } finally {
                   //inStream.close();
        }  
           
    }
    
    public StreamedContent getFile() {
        return file;
    }
            
    public void setFile(StreamedContent file){
        this.file = file;
    }
    
    public String getContentType() {
        return file.getContentType();
    }
}
