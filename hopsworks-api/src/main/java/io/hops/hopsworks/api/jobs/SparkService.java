/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import com.google.common.base.Strings;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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

import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import javax.ws.rs.core.SecurityContext;

/**
 * Service offering functionality to run a Spark fatjar job.
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SparkService {

  private static final Logger LOGGER = Logger.getLogger(SparkService.class.
      getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private SparkController sparkController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobController jobController;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;

  SparkService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project of type Spark.
   * <p/>
   * @return A list of all Jobs objects of type Spark in this project.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllSparkJobs() {
    List<Jobs> jobs = jobFacade.findJobsForProjectAndType(project, JobType.SPARK);
    GenericEntity<List<Jobs>> jobList = new GenericEntity<List<Jobs>>(jobs) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(jobList).build();
  }

  /**
   * Inspect a jar in HDFS prior to running a job. Returns a
   * SparkJobConfiguration object.
   * <p/>
   * @param path
   * @param sc
   * @return
   * @throws io.hops.hopsworks.common.exception.JobException
   */
  @GET
  @Path("/inspect/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response inspectJar(@PathParam("path") String path, @Context SecurityContext sc) throws JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      SparkJobConfiguration config = sparkController.inspectProgram(path, username, udfso);
      //SparkJobConfiguration config = sparkController.inspectProgram(path, username, req.getSession().getId());
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(config).build();
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  /**
   * Create a new Job definition. If successful, the job is returned.
   * <p/>
   * @param config The configuration from which to create a Job.
   * @param sc
   * @return
   * @throws io.hops.hopsworks.common.exception.JobException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createJob(SparkJobConfiguration config, @Context SecurityContext sc) throws JobException {
    if (config == null) {
      throw new IllegalArgumentException("Job configuration was not provided.");
    } else {
      Users user = jWTHelper.getUserPrincipal(sc);

      if (Strings.isNullOrEmpty(config.getAppName())) {
        throw new IllegalArgumentException("Job name was not provided.");
      } else if (!HopsUtils.jobNameValidator(config.getAppName(), Settings.FILENAME_DISALLOWED_CHARS)) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_NAME_INVALID, Level.FINE, "job name: " +config.getAppName());
      }
      Jobs created = jobController.createJob(user, project, config);
      activityFacade.persistActivity(ActivityFacade.CREATED_JOB + created.getName(), project, user);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(created).build();
    }
  }
}
