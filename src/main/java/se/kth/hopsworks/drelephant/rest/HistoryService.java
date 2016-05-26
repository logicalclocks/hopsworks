package se.kth.hopsworks.drelephant.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.security.RolesAllowed;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import se.kth.hopsworks.filters.AllowedRoles;

@Path("/history")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)

public class HistoryService {
    
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.ALL})
    public String getAllProjects(@Context SecurityContext sc,
        @Context HttpServletRequest req) {

        try{
            URL url = new URL("http://bbc1.sics.se:21001/rest/job?id=application_1464164365100_0001");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200){
                throw new RuntimeException("Failed HTTP Request: " + conn.getResponseCode());
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            String output;            
            while ((output = br.readLine()) != null) {
			System.out.println(output);
		}
            
            conn.disconnect();
            return output;
        }
        catch (MalformedURLException e) {
		e.printStackTrace();
        } 
        catch (IOException e) {
        e.printStackTrace();
        }
        return null;
    }
    
    
    
//    public static void main (String[] args){
//    
//        try{
//        
//            URL url = new URL("http://bbc1.sics.se:21001/rest/job?id=application_1464164365100_0001");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Accept", "application/json");
//            
//            if (conn.getResponseCode() != 200){
//                throw new RuntimeException("Failed HTTP Request: " + conn.getResponseCode());
//            }
//            
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            
//            String output;
//            System.out.println("The output from the server");
//            
//            while ((output = br.readLine()) != null) {
//			System.out.println(output);
//		}
//            
//            conn.disconnect();
//            
//        }
//        
//        catch (MalformedURLException e) {
//
//		e.printStackTrace();
//
//	  } catch (IOException e) {
//
//		e.printStackTrace();
//
//	  }
//            
//        }
    }
    

