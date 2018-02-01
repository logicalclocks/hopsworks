/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import com.google.common.base.Strings;
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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;

/**
 * Service offering functionality to run a Flink fatjar job.
 * <p>
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
  private JobFacade jobFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobController jobController;
  @EJB
  private HdfsUsersController hdfsUsersBean;
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
   * @return A list of all Jobs objects of type Flink in this
 project.
   * @throws AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findAllFlinkJobs(@Context SecurityContext sc,
      @Context HttpServletRequest req)
      throws AppException {
    List<Jobs> jobs = jobFacade.findJobsForProjectAndType(project,
        JobType.FLINK);
    GenericEntity<List<Jobs>> jobList
        = new GenericEntity<List<Jobs>>(jobs) {};
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
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
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  /**
   * Create a new Job definition. If successful, the job is returned.
   * <p/>
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

      if (user == null) {
        //Should not be possible, but, well...
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
            "You are not authorized for this invocation.");
      }
      if (Strings.isNullOrEmpty(config.getAppName())) {
        config.setAppName("Untitled Flink job");
      } else if (!HopsUtils.jobNameValidator(config.getAppName(), Settings.FILENAME_DISALLOWED_CHARS)) {
        throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
            "Invalid charater(s) in job name, the following characters (including space) are now allowed:"
            + Settings.FILENAME_DISALLOWED_CHARS);
      }
      Jobs created = jobController.createJob(user, project, config);
      activityFacade.persistActivity(ActivityFacade.CREATED_JOB, project, email);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(created).build();
    }
  }
}
