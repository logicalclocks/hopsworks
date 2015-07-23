package se.kth.hopsworks.rest;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.jobhistory.Job;
import se.kth.bbc.jobs.jobhistory.JobFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.CuneiformController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CuneiformService {

  @EJB
  private CuneiformController cfCtrl;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private UserFacade userFacade;

  private Project project;

  CuneiformService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project of type cuneiform.
   * <p>
   * @param sc
   * @param req
   * @return A list of all Job objects of type Cuneiform in this project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllCuneiformJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<Job> jobs = jobFacade.findForProjectByType(project, JobType.CUNEIFORM);
    GenericEntity<List<Job>> jobList = new GenericEntity<List<Job>>(jobs) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jobList).build();
  }

  /**
   * Inspect the workflow stored at the given path. Returns a
   * CuneiformJobConfiguration containing a WorkflowDTO with the workflow
   * details and a YarnJobConfiguration with Yarn job config.
   * <p>
   * @param path
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/inspect/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response inspectStoredWorkflow(@PathParam("path") String path,
          @Context SecurityContext sc, @Context HttpServletRequest req)
          throws AppException {
    try {
      WorkflowDTO wf = cfCtrl.inspectWorkflow(path);
      CuneiformJobConfiguration ret = new CuneiformJobConfiguration(wf);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(ret).build();
    } catch (IOException |
            HasFailedException ex) {
      Logger.getLogger(CuneiformService.class.getName()).log(Level.SEVERE,
              "Error upon inspecting workflow.",
              ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Failed to inspect the workflow file.");
    } catch (IllegalArgumentException ex) {
      Logger.getLogger(CuneiformService.class.getName()).log(Level.WARNING,
              "Error upon inspecting workflow:",
              ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Failed not inspect the workflow file. Reason:" + ex.getMessage());
    }
  }

  /**
   * Create a new Cuneiform job. The CuneiformJobConfiguration is passed as an
   * argument. This call returns the created Job object. To see if the job was
   * started, update the execution list.
   * <p>
   * @param runData
   * @param sc
   * @param req
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response createJob(CuneiformJobConfiguration runData,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    //First: argument checking
    if (runData == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    //Get the info to build a Job object
    String jobname = runData.getAppName();
    JobType type = JobType.CUNEIFORM;
    String email = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(email);
    //Create the new job object.
    Job job = jobFacade.create(jobname, user, project, type, runData);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            job).build();
  }
  
  

}
