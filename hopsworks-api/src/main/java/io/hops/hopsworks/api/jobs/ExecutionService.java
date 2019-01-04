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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExecutionService {

  private static final Logger LOGGER = Logger.getLogger(ExecutionService.class.getName());

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ExecutionController executionController;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private JWTHelper jWTHelper;

  private Jobs job;

  ExecutionService setJob(Jobs job) {
    this.job = job;
    return this;
  }

  /**
   * Get all the executions for the given job.
   * <p/>
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAllExecutions() {
    List<Execution> executions = executionFacade.findForJob(job);
    GenericEntity<List<Execution>> list = new GenericEntity<List<Execution>>(executions) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(list).build();
  }

  /**
   * Start an Execution of the given job.
   * <p/>
   * @param sc
   * @return The new execution object.
   * @throws io.hops.hopsworks.common.exception.ProjectException
   * @throws io.hops.hopsworks.common.exception.GenericException
   * @throws io.hops.hopsworks.common.exception.JobException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response startExecution(@Context SecurityContext sc) throws ProjectException, GenericException,
      JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if(job.getProject().getPaymentType().equals(PaymentType.PREPAID)){
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(job.getProject().getName());
      if(projectQuota==null || projectQuota.getQuotaRemaining() < 0){
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_ERROR, Level.FINE);
      }
    }
    Execution exec = executionController.start(job, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(exec).build();
  }

  @POST
  @Path("/stop")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopExecution(@PathParam("jobId") int jobId, @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);

    executionController.kill(job, user);
    //executionController.stop(job, user, appid);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity("Job stopped").build();
  }

  /**
   * Get the execution with the specified id under the given job.
   * <p/>
   * @param executionId
   * @return
   * @throws io.hops.hopsworks.common.exception.JobException
   */
  @GET
  @Path("/{executionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getExecution(@PathParam("executionId") int executionId) throws JobException {
    Execution execution = executionFacade.findById(executionId);
    if (execution == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } else if (!execution.getJob().equals(job)) {
      //The user is requesting an execution that is not under the given job. May be a malicious user!
      throw new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, Level.FINE,
        "Trying to access an execution of another job");
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(execution).build();
    }

  }

}
