package se.kth.hopsworks.drelephant.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.jobs.jobhistory.YarnAppResult;
import se.kth.bbc.jobs.jobhistory.YarnAppResultFacade;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.JsonResponse;
import se.kth.hopsworks.rest.NoCacheResponse;


@Path("history")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HistoryService {
    

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private YarnAppResultFacade yarnAppResultFacade;
    
  
  @GET
  @Path("all")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
    public Response getAllProjects(@Context SecurityContext sc,
        @Context HttpServletRequest req) throws AppException{
        
    List<YarnAppResult> appResults = yarnAppResultFacade.findAllHistory();
    GenericEntity<List<YarnAppResult>> yarnApps
        = new GenericEntity<List<YarnAppResult>>(appResults) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        yarnApps).build();
    }
    
    
  @GET
  @Path("details/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getJob(@PathParam("jobId") String jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req,
      @HeaderParam("Access-Control-Request-Headers") String requestH) throws AppException{
        
        try {
		URL url = new URL("http://bbc1.sics.se:21001/rest/job?id=" + jobId );
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
			(conn.getInputStream())));

                String output;
                StringBuilder outputBuilder = new StringBuilder();
                JsonResponse json = new JsonResponse();
		while ((output = br.readLine()) != null) {
                        outputBuilder.append(output);
		}
                
                json.setData(outputBuilder);
                json.setStatus("OK");
                json.setSuccessMessage(ResponseMessages.JOB_DETAILS);
		conn.disconnect();
                return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();

        } catch (MalformedURLException e) {
		e.printStackTrace();
        } catch (IOException e) {
		e.printStackTrace();
        }
        return null;
    }
}
    

