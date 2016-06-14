package se.kth.hopsworks.drelephant.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
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
import se.kth.bbc.jobs.jobhistory.HeuristicsBean;
import se.kth.bbc.jobs.jobhistory.YarnAppResult;
import se.kth.bbc.jobs.jobhistory.YarnAppResultFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.JobService;
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
  @EJB
  private ProjectFacade projectFacade;
  @Inject
  private JobService jobs;
  @EJB
  private JobDescriptionFacade jobFacade;
    
  
  @GET
  @Path("all/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
    public Response getAllProjects(@PathParam("projectId") int projectId,   
        @Context SecurityContext sc,
        @Context HttpServletRequest req) throws AppException{
        
    Project returnProject = projectFacade.find(projectId);
        
    List<YarnAppResult> appResults = yarnAppResultFacade.findByUsername(returnProject.getName() + "__meb10000");
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
        
      
        //TODO: Change the URL 
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
  
  
  @GET
  @Path("heuristics/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getHeuristic(@PathParam("jobId") Integer jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req,
      @HeaderParam("Access-Control-Request-Headers") String requestH) throws AppException{
            
        //Rerutn jsonObject with the response of the Heuristics
        String jsonInString = "";
        
        HeuristicsBean hb = new HeuristicsBean();
        hb.setMemory(15);
        hb.setvCores(1);
        hb.setName("HB_NAME");
        hb.setSize(20);
        hb.setClassName("CLASS_HB");
        
        JobDescription jd = jobFacade.findById(jobId);
        hb.setJobType(jd.getJobType().getName());
        
        ObjectMapper om = new ObjectMapper();
        
        try {
            jsonInString = om.writeValueAsString(hb);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(HistoryService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        JsonResponse json = new JsonResponse();
        json.setData(jsonInString);
        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.JOB_DETAILS);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }
}
    

