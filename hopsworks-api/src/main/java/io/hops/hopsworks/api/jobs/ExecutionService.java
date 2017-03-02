package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
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
import org.slf4j.LoggerFactory;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExecutionService {

  private static final Logger LOG = Logger.getLogger(ExecutionService.class.getName());

  private static final org.slf4j.Logger debugger = LoggerFactory.getLogger(
      ExecutionController.class);

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private ExecutionController executionController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private Settings settings;

  private JobDescription job;

  ExecutionService setJob(JobDescription job) {
    this.job = job;
    return this;
  }

  /**
   * Get all the executions for the given job.
   * <p/>
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getAllExecutions(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    List<Execution> executions = executionFacade.findForJob(job);
    GenericEntity<List<Execution>> list = new GenericEntity<List<Execution>>(
        executions) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(list).build();
  }

  /**
   * Start an Execution of the given job.
   * <p/>
   * @param sc
   * @param req
   * @return The new execution object.
   * @throws AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response startExecution(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    try {
      Execution exec = executionController.start(job, user);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(exec).build();
    } catch (IOException | IllegalArgumentException | NullPointerException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "An error occured while trying to start this job: " + ex.
              getLocalizedMessage());
    }
  }

  @POST
  @Path("/stop")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response stopExecution(@PathParam("jobId") int jobId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "You are not authorized for this invocation.");
    }
    job = jobFacade.findById(jobId);
    String appid = yarnApplicationstateFacade.findByAppname(job.getName())
        .get(0)
        .getApplicationid();

    //Look for unique marker file which means it is a streaming job. Otherwise proceed with normal kill.
    DistributedFileSystemOps udfso = null;
    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    try {
      udfso = dfs.getDfsOps(username);
      String marker = File.separator + "Projects" + File.separator + job.getProject().getName() + File.separator
          + "Resources" + File.separator + ".marker-" + job.getJobType().getName().toLowerCase() + "-" + job.getName()
          + "-" + appid;

      if (udfso.exists(marker)) {
        udfso.rm(new org.apache.hadoop.fs.Path(marker), false);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity("Job stopped").build();
      }
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not remove marker file for job:" + job.getName() + "with appId:" + appid, ex);
    } finally {
      if (udfso != null) {
        udfso.close();
      }
    }

    try {
      //WORKS FOR NOW BUT SHOULD EVENTUALLY GO THROUGH THE YARN CLIENT API
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(settings.getHadoopDir() + "/bin/yarn application -kill " + appid);

      //executionController.stop(job, user, appid);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity("Job stopped").build();
    } catch (IOException | IllegalArgumentException | NullPointerException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "An error occured while trying to start this job: " + ex.
              getLocalizedMessage());
    }
  }

  /**
   * Get the execution with the specified id under the given job.
   * <p/>
   * @param executionId
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{executionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getExecution(@PathParam("executionId") int executionId,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws
      AppException {
    Execution execution = executionFacade.findById(executionId);
    if (execution == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } else if (!execution.getJob().equals(job)) {
      //The user is requesting an execution that is not under the given job. May be a malicious user!
      LOG.log(Level.SEVERE,
          "Someone is trying to access an execution under a job where it does "
          + "not belong. May be a malicious user!");
      return Response.status(Response.Status.FORBIDDEN).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(execution).build();
    }

  }

}
