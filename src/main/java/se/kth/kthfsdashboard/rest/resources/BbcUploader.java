/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.rest.resources;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.web.WebHdfsFileSystem;



/**
 * REST Web Service
 *
 * @author roshan
 */
@Path("/bbcuploader")
@RequestScoped
@RolesAllowed({"BBC_ADMIN","BBC_RESEARCHER"})
public class BbcUploader {

      
    
    @Context
    private UriInfo context;

    private final String nameHost = "http://snurran.sics.se:50070/webhdfs/v1";
    /**
     * Creates a new instance of BbcUploader
     */
    public BbcUploader() {
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("fileName") FormDataContentDisposition fileDisposition){
        
            String fileName = fileDisposition.getFileName();
            String url = nameHost+"/user/roshan/"+fileName+"?user.name=glassfish&op=CREATE&data=true";
                     
            return Response.status(200).entity("OK").build();
       
    
    }     
    
    
//    public void writeLocal(InputStream incoming, String location) {
//    
//        try {
//        
//            OutputStream outstream = new FileOutputStream(new File(location));
//             
//            int read = 0;
//            byte[] buffer = new byte[10240];
//            while((read = incoming.read(buffer)) != -1){
//                    outstream.write(buffer, 0, read);
//            } 
//                    outstream.flush();
//                    outstream.close();
//                    
//            }catch(IOException iox){
//                    System.err.println("IOException during operation"+ iox.toString());
//                    System.exit(1);
//              }
//              
//             
//        
//        }
    
//    public void uploadFileToHDFS(InputStream in, String fileToCreate){
//    
//        Configuration conf = new Configuration();
//        //conf.addResource(new org.apache.hadoop.fs.Path("/home/glassfish/roshan/hadoop-2.2.0/etc/hadoop/core-site.xml"));
//        //conf.addResource(new org.apache.hadoop.fs.Path("/home/glassfish/roshan/hadoop-2.2.0/etc/hadoop/hdfs-site.xml"));
//        
//        conf.set("fs.defaultFS", this.nameNURI);
//        OutputStream out;
//                      
//        try {
//            
//            DFSClient dfsClient = new DFSClient(new URI(this.nameNURI),conf);
//            
//            if(dfsClient.exists(fileToCreate)){
//                System.out.println("Error: File exists! "+fileToCreate);
//                return;
//            }
//            
//            out = new BufferedOutputStream(dfsClient.create(fileToCreate, false));
//            
//            byte[] buffer = new byte[10240];
//            int len = 0;
//            while ((len = in.read(buffer)) != -1) {
//                out.write(buffer, 0, len);
//            }
//            
//            dfsClient.close();
//            out.flush();
//            out.close();
//                  
//        }catch(IOException iox) {
//            System.err.println("IOException during operation"+ iox.toString());
//            System.exit(1);
//        }catch(URISyntaxException uri){
//            System.err.println("URISyntaxException during operation"+ uri.toString());
//            System.exit(1);
//        }
//            System.out.println("Copied to HDFS "+ this.nameNURI +"/" +fileToCreate);
//        
//    }
    
        
    
    @GET
    @Path("/read")
    @Produces("text/plain")
    public String downloadFile() {
        
        
        //TODO return proper representation object
        //throw new UnsupportedOperationException();
            return "Invoked. Hello World!";
    
    }

    /**
     * PUT method for updating or creating an instance of BbcUploader
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Path("/update")
    @Consumes("text/plain")
    public void putText(String content) {
    }
    
    
    
}
