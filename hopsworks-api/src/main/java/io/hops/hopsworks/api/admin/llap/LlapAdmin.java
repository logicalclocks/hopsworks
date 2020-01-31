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

package io.hops.hopsworks.api.admin.llap;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.admin.llap.LlapClusterFacade;
import io.hops.hopsworks.common.admin.llap.LlapClusterLifecycle;
import io.hops.hopsworks.common.admin.llap.LlapClusterStatus;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;

@Path("/admin/llap")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LlapAdmin {

  @EJB
  private LlapClusterFacade llapClusterFacade;
  @EJB
  private LlapClusterLifecycle llapClusterLifecycle;
  @EJB
  private NoCacheResponse noCacheResponse;

  /**
   * Return the state of the llap cluster and other information
   * such as the appId and the hosts on which the cluster is running
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response clusterStatus(@Context SecurityContext sc) {
    LlapClusterStatus status = llapClusterFacade.getClusterStatus();
    GenericEntity<LlapClusterStatus> statusEntity =
        new GenericEntity<LlapClusterStatus>(status) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(statusEntity).build();
  }

  /**
   * Update the state of the cluster based on the ingested JSON
   * @param llapClusterRequest
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeClusterStatus(LlapClusterStatus llapClusterRequest, @Context SecurityContext sc)
    throws ServiceException {
    LlapClusterStatus oldClusterStatus = llapClusterFacade.getClusterStatus();
    LlapClusterStatus.Status desiredStatus = llapClusterRequest.getClusterStatus();

    switch (desiredStatus) {
      case UP:
        if (oldClusterStatus.getClusterStatus() == desiredStatus) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.LLAP_CLUSTER_ALREADY_UP, Level.WARNING);
        }
        llapClusterLifecycle.startCluster(llapClusterRequest.getInstanceNumber(),
            llapClusterRequest.getExecutorsMemory(),
            llapClusterRequest.getCacheMemory(),
            llapClusterRequest.getExecutorsPerInstance(),
            llapClusterRequest.getIOThreadsPerInstance());
        break;
      case DOWN:
        if (oldClusterStatus.getClusterStatus() == desiredStatus) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.LLAP_CLUSTER_ALREADY_DOWN, Level.WARNING);
        }
        llapClusterLifecycle.stopCluster();
        break;
      default:
        throw new ServiceException(RESTCodes.ServiceErrorCode.LLAP_STATUS_INVALID, Level.WARNING,
          "status: " + desiredStatus);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).build();
  }
}
