package io.hops.hopsworks.api.jobs;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.NoCacheResponse;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.tensorflow.TensorFlowController;
import io.hops.hopsworks.common.jobs.tensorflow.TensorFlowJobConfiguration;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.util.logging.Level;
import javax.ws.rs.Consumes;
import org.apache.hadoop.security.AccessControlException;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorFlowService {

  private final static Logger LOGGER = Logger.getLogger(TensorFlowService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  @EJB
  private TensorFlowController tensorflowController;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private Settings settings;
  @EJB
  private JobController jobController;

  private Integer projectId;
  private Project project;

  public TensorFlowService() {
  }

  TensorFlowService setProject(Project project) {
    this.project = project;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }


  /**
   * Inspect a python in HDFS prior to running a job. Returns a
   * TensorFlowJobConfiguration object.
   *
   * @param path
   * @param sc
   * @param req
   * @return
   * @throws AppException
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @GET
  @Path("/inspect/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response inspectProgram(@PathParam("path") String path,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws
      AppException, AccessControlException {
    String email = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(email);
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      TensorFlowJobConfiguration config = tensorflowController.inspectProgram(path, username, udfso);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(config).build();
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You do not have access to the jar file.");
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Failed to inspect file.", ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Error reading file: " + ex.getLocalizedMessage());
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.WARNING, "Got a non-python file to inspect.");
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Error reading jar file: " + e.getLocalizedMessage());
    } finally {
      if (udfso != null) {
        udfso.close();
      }
    }
  }

  /**
   * Create a new Job definition. If successful, the job is returned.
   *
   * @param config The configuration from which to create a Job.
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response createJob(TensorFlowJobConfiguration config,
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

      if (Strings.isNullOrEmpty(config.getAppName())) {
        config.setAppName("Untitled TensorFlow job");
      } else if (!HopsUtils.jobNameValidator(config.getAppName(), Settings.FILENAME_DISALLOWED_CHARS)) {
        throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
            "Invalid charater(s) in job name, the following characters (including space) are now allowed:"
            + Settings.FILENAME_DISALLOWED_CHARS);
      }
      if (Strings.isNullOrEmpty(config.getAnacondaDir())) {
        config.setAnacondaDir(settings.getAnacondaProjectDir(project.getName()));
      }
      Jobs created = jobController.createJob(user, project, config);
      activityFacade.persistActivity(ActivityFacade.CREATED_JOB + created.getName(), project, email);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(created).build();
    }
  }

}
