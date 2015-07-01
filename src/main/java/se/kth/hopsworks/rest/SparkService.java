package se.kth.hopsworks.rest;

import java.io.IOException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.hopsworks.controller.SparkController;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 * Service offering functionality to run a Spark fatjar job.
 * <p>
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SparkService {

  private static final Logger logger = Logger.getLogger(SparkService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private SparkController sparkController;

  private Integer projectId;

  SparkService setProjectId(Integer id) {
    this.projectId = id;
    return this;
  }

  /**
   * Inspect a jar in HDFS prior to running a job. Returns a
   * SparkJobConfiguration object.
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
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response inspectJar(@PathParam("path") String path,
          @Context SecurityContext sc, @Context HttpServletRequest req) throws
          AppException {
    try {
      SparkJobConfiguration config = sparkController.inspectJar(path);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(config).build();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to inspect jar.", ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error reading jar file: " + ex.
              getLocalizedMessage());
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Got a non-jar file to inspect as Spark jar.");
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error reading jar file: " + e.
              getLocalizedMessage());
    }
  }

  /**
   * Run a job. This method returns a JobHistory object if it succeeds.
   * <p>
   * @param config
   * @param req
   * @param sc
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Path("/run")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response runJob(SparkJobConfiguration config,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    if (config == null || config.getJarPath() == null || config.getJarPath().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You must set a jar path before running.");
    } else if (config.getMainClass() == null || config.getMainClass().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You must set a main class before running.");
    }
    try {
      if (!config.getJarPath().startsWith("hdfs")) {
        config.setJarPath("hdfs://" + config.getJarPath());
      }
      JobHistory jh = sparkController.startJob(config, req.getUserPrincipal().
              getName(), projectId);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(jh).build();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error running Spark job.", ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error running job: " + ex.getLocalizedMessage());
    }
  }
}
