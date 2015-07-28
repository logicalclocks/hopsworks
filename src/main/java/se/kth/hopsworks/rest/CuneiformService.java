package se.kth.hopsworks.rest;

import com.google.common.base.Strings;
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
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.jobs.model.description.CuneiformJobDescription;
import se.kth.bbc.jobs.model.description.JobDescription;
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
  private JobDescriptionFacade jobFacade;
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
   * @return A list of all JobDescription objects of type Cuneiform in this
   * project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllCuneiformJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<CuneiformJobDescription> jobs = jobFacade.
            findCuneiformJobsForProject(project);
    GenericEntity<List<CuneiformJobDescription>> jobList
            = new GenericEntity<List<CuneiformJobDescription>>(
                    jobs) {
                    };
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                    entity(jobList).build();
  }

  /**
   * Inspect the workflow stored at the given path. Returns a
   * CuneiformJobConfiguration containing a WorkflowDTO with the workflow
   * details and Yarn job config.
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
   * Create a new Job definition. If successful, the job is returned.
   * <p>
   * @param config The configuration from which to create a Job.
   * @param sc
   * @param req
   * @return The newly created job object.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response createJob(CuneiformJobConfiguration config,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    if (config == null) {
      throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
              "Cannot create job for a null argument.");
    } else {
      String email = sc.getUserPrincipal().getName();
      Users user = userFacade.findByEmail(email);
      if (user == null) {
        //Should not be possible, but, well...
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "You are not authorized for this invocation.");
      }
      String jobname = Strings.isNullOrEmpty(config.getAppName())
              ? "Untitled Cuneiform job" : config.getAppName();
      JobDescription<? extends JobConfiguration> created = jobFacade.create(
              jobname, user, project, config);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(created).build();
    }
  }
}
