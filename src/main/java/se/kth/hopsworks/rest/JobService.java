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
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ProjectController;
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
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JobHistoryFacade jobHistoryFacade;
  @Inject
  private CuneiformService cuneiform;
  @Inject
  private SparkService spark;

  private Integer projectId;

  JobService setProjectId(Integer id) {
    this.projectId = id;
    return this;
  }

  /**
   * Get all the jobhistory objects in this project with the specified type.
   * <p>
   * @param type The type of jobs to fetch. The String parameter passed through
   * REST should be an uppercase version of the constant value.
   * @param sc
   * @param reqJobType
   * @return A list of all jobhistory objects.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Path("/history/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllJobHistoryByType(@PathParam("type") JobType type,
          @Context SecurityContext sc, @Context HttpServletRequest reqJobType)
          throws AppException {
    Project project = projectController.findProjectById(projectId);
    List<JobHistory> history = jobHistoryFacade.findForProjectByType(project,
            type);
    GenericEntity<List<JobHistory>> jobHistory
            = new GenericEntity<List<JobHistory>>(history) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            jobHistory).build();
  }

  @GET
  @Path("/status/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getStatusOfJob(@PathParam("jobId") long id,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException {
    JobHistory jh = jobHistoryFacade.find(id);
    if (jh == null) {
      logger.log(Level.WARNING,
              "Trying to access the status of a non-existing job with id {0}",
              id);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "No job with the given id was found.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            new GenericEntity<JobHistory>(jh) {
            }).build();
  }

  @Path("/cuneiform")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public CuneiformService cuneiform() {
    return this.cuneiform.setProjectId(projectId);
  }

  @Path("/spark")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public SparkService spark() {
    return this.spark.setProjectId(projectId);
  }
}
