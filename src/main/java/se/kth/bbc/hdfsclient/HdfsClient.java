/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.hdfsclient;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSClient;

/**
 *
 * @author roshan
 */

@ManagedBean
@RequestScoped
public class HdfsClient {
    
    public final String nameNodeURI = "hdfs://snurran.sics.se/9999";
    
    public void mkDIRS(String rootDir) throws IOException, URISyntaxException{
    
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        DFSClient client = new DFSClient(new URI(this.nameNodeURI), conf);
        
        try {
            if (client.exists(rootDir)) {
                System.out.println("Directory structured is exists! " + rootDir);
                return;
            }
        
            client.mkdirs(rootDir, null, false);
            
         } catch(IOException ioe){
            System.err.println("IOException during operation"+ ioe.toString());
            System.exit(1);
         }finally {
            
            client.close();
        
        }
        
        
    }
    
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
    
    
    
    
}
