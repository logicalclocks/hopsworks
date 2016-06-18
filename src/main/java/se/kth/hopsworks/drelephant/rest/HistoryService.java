package se.kth.hopsworks.drelephant.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.json.JSONObject;
import se.kth.bbc.jobs.jobhistory.JobDetailDTO;
import se.kth.bbc.jobs.jobhistory.JobHeuristicDTO;
import se.kth.bbc.jobs.jobhistory.JobHeuristicDetailsDTO;
import se.kth.bbc.jobs.jobhistory.JobsHistoryFacade;
import se.kth.bbc.jobs.jobhistory.YarnAppResult;
import se.kth.bbc.jobs.jobhistory.YarnAppResultFacade;
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
    
  private static final String DR_ELEPHANT_ADDRESS = "http://bbc1.sics.se:18001";  

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
  @EJB
  private JobsHistoryFacade jobsHistoryFacade;
    
  
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

        JsonResponse json = getJobDetailsFromDrElephant(jobId); 
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

  
  @POST
    @Path("heuristics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
    public Response Heuristics(JobDetailDTO jobDetailDTO,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {
        
        JobHeuristicDTO jobsHistoryResult = jobsHistoryFacade.searchHeuristicRusults(jobDetailDTO);
        
        Iterator<String> jobIt = jobsHistoryResult.getSimilarAppIds().iterator();
        
        while(jobIt.hasNext()){
            String appId = jobIt.next();
            JsonResponse json = getJobDetailsFromDrElephant(appId);
        
            StringBuilder jsonString = (StringBuilder) json.getData();
            JSONObject jsonObj = new JSONObject(jsonString.toString());
        
            String totalSeverity = jsonObj.get("severity").toString();
            
            JobHeuristicDetailsDTO jhD = new JobHeuristicDetailsDTO(appId, totalSeverity);
            jobsHistoryResult.addJobHeuristicDetails(jhD);
        }
        
        GenericEntity<JobHeuristicDTO> jobsHistory = new GenericEntity<JobHeuristicDTO>(jobsHistoryResult){};
        
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        jobsHistory).build();
    }
    
    
    private JsonResponse getJobDetailsFromDrElephant(String jobId){
    
    try {
		URL url = new URL(DR_ELEPHANT_ADDRESS + "/rest/job?id=" + jobId );
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
                return json;

        } catch (MalformedURLException e) {
		e.printStackTrace();
        } catch (IOException e) {
		e.printStackTrace();
        }
        
        return null;
    }
  
}
    

