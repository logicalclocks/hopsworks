/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.rest.resources;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


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

    /**
     * Creates a new instance of BbcUploader
     */
    public BbcUploader() {
    }

    /**
     * Retrieves representation of an instance of se.kth.bbc.upload.BbcUploader
     * @return an instance of java.lang.String
     */
    
    @POST
    @Path("/upload/{fileName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("fileName") InputStream fileName, @FormDataParam("fileName") FormDataContentDisposition fileDisposition){
        
            String filePath = "/home/glassfish/roshan/uploads/" + fileDisposition.getFileName();
            writeLocal(fileName, filePath);
            
            String message = "Done :" + filePath;
            
            return Response.status(200).entity(message).build();
       
    
    }     
    
    
    public void writeLocal(InputStream incoming, String location) {
    
        try {
        
            OutputStream outstream = new FileOutputStream(new File(location));
             
            int read = 0;
            byte[] buffer = new byte[10240];
            while((read = incoming.read(buffer)) != -1){
                    outstream.write(buffer, 0, read);
            } 
                    outstream.flush();
                    outstream.close();
            }catch(IOException iox){
                    iox.printStackTrace();
              }
              
              
        
        }
    
    
    
    
    
//    @GET
//    @Produces("text/plain")
//    public String downloadFile() {
//        //TODO return proper representation object
//        throw new UnsupportedOperationException();
//    }

    /**
     * PUT method for updating or creating an instance of BbcUploader
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
//    @PUT
//    @Consumes("text/plain")
//    public void putText(String content) {
//    }
    
    
    
}
