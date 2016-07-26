package se.kth.hopsworks.rest;

import com.google.common.base.Strings;
import io.hops.hdfs.HdfsLeDescriptors;
import io.hops.hdfs.HdfsLeDescriptorsFacade;
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
import org.apache.hadoop.security.AccessControlException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.jobs.flink.FlinkJobConfiguration;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.FlinkController;
import se.kth.hopsworks.controller.JobController;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

/**
 * Service offering functionality to run a Flink fatjar job.
 * <p/>
 * 
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FlinkService {

    private static final Logger logger = Logger.getLogger(FlinkService.class.
            getName());

    @EJB
    private NoCacheResponse noCacheResponse;
    @EJB
    private FlinkController flinkController;
    @EJB
    private JobDescriptionFacade jobFacade;
    @EJB
    private UserFacade userFacade;
    @EJB
    private ActivityFacade activityFacade;
    @EJB
    private JobController jobController;
    @EJB
    private HdfsUsersController hdfsUsersBean;
    @EJB
    private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
    @EJB
    private DistributedFsService dfs;
    
    private Project project;

    FlinkService setProject(Project project) {
        this.project = project;
        return this;
    }

    /**
     * Get all the jobs in this project of type Flink.
     * <p/>
     * @param sc
     * @param req
     * @return A list of all JobDescription objects of type Flink in this
     * project.
     * @throws se.kth.hopsworks.rest.AppException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
    public Response findAllFlinkJobs(@Context SecurityContext sc,
            @Context HttpServletRequest req)
            throws AppException {
        List<JobDescription> jobs = jobFacade.findJobsForProjectAndType(project,
                JobType.FLINK);
        GenericEntity<List<JobDescription>> jobList
                = new GenericEntity<List<JobDescription>>(jobs) {
                };
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(jobList).build();
    }

    /**
     * Inspect a jar in HDFS prior to running a job. Returns a
     * FlinkJobConfiguration object.
     * <p/>
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
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response inspectJar(@PathParam("path") String path,
            @Context SecurityContext sc, @Context HttpServletRequest req) throws
            AppException, AccessControlException {
        String email = sc.getUserPrincipal().getName();
        Users user = userFacade.findByEmail(email);
        String username = hdfsUsersBean.getHdfsUserName(project, user);
        DistributedFileSystemOps udfso = null;
        try {
          udfso = dfs.getDfsOps(username);
          FlinkJobConfiguration config = flinkController.inspectJar(path,
                  username, udfso);
          return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                    entity(config).build();
        } catch (AccessControlException ex) {
            throw new AccessControlException(
                    "Permission denied: You do not have access to the jar file.");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to inspect jar.", ex);
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                    getStatusCode(), "Error reading jar file: " + ex.
                    getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Got a non-jar file to inspect as Flink jar.");
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                    getStatusCode(), "Error reading jar file: " + e.
                    getLocalizedMessage());
        }finally{
          udfso.close();
        }
    }

    /**
     * Create a new Job definition. If successful, the job is returned.
     * <p/>
     * @param config The configuration from which to create a Job.
     * @param sc
     * @param req
     * @return
     * @throws se.kth.hopsworks.rest.AppException
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
    public Response createJob(FlinkJobConfiguration config,
            @Context SecurityContext sc,
            @Context HttpServletRequest req) throws AppException {
        if (config == null) {
            throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
                    "Cannot create job for a null argument.");
        } else {
            String email = sc.getUserPrincipal().getName();
            Users user = userFacade.findByEmail(email);
            String path = config.getJarPath();
            if (!path.startsWith("hdfs")) {
                path = "hdfs://" + path;
            }
            HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();
            path = path.replaceFirst("hdfs:/*Projects",
                    "hdfs://" + hdfsLeDescriptors.getHostname() + "/Projects");

            if (user == null) {
                //Should not be possible, but, well...
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                        "You are not authorized for this invocation.");
            }
            if (Strings.isNullOrEmpty(config.getAppName())) {
                config.setAppName("Untitled Flink job");
            }
            JobDescription created = jobController.createJob(user, project, config);
            activityFacade.persistActivity(ActivityFacade.CREATED_JOB, project, email);
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                    entity(created).build();
        }
    }
}
