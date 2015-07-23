package se.kth.hopsworks.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.jobs.jobhistory.JobDescription;
import se.kth.bbc.jobs.jobhistory.JobFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobService {

  private static final Logger logger = Logger.getLogger(JobService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionService executions;
  @Inject
  private CuneiformService cuneiform;
  @Inject
  private SparkService spark;
  @Inject
  private AdamService adam;

  private Project project;

  JobService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project.
   * <p>
   * @param sc
   * @param req
   * @return A list of all defined Jobs in this project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<JobDescription> jobs = jobFacade.findForProject(project);
    GenericEntity<List<JobDescription>> jobList = new GenericEntity<List<JobDescription>>(jobs) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jobList).build();
  }

  /**
   * Get the job with the given id in the current project.
   * <p>
   * @param jobId
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getJob(@PathParam("jobId") int jobId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(job).build();
    }
  }

  /**
   * Get the ExecutionService for the job with given id.
   * @param jobId
   * @return 
   */
  @Path("/{jobId}/executions")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public ExecutionService executions(@PathParam("jobId") int jobId) {
    JobDescription job = jobFacade.findById(jobId);
    if (job == null) {
      return null;
    } else if (!job.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      logger.log(Level.SEVERE,
              "A user is trying to access a job outside their project!");
      return null;
    } else {
      return this.executions.setJob(job);
    }
  }

  @Path("/cuneiform")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public CuneiformService cuneiform() {
    return this.cuneiform.setProject(project);
  }

  @Path("/spark")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public SparkService spark() {
    return this.spark.setProjectId(projectId);
  }

  @Path("/adam")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public AdamService adam() {
    return this.adam.setProjectId(projectId);
  }
}
