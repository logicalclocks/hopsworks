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

package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaHdfsController;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/remote/dela")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Cross Dela Service",
  description = "Cross Dela Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteDelaService {

  private static final Logger LOGGER = Logger.getLogger(RemoteDelaService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private DelaHdfsController hdfsDelaCtrl;
  @EJB
  private DatasetFacade datasetFacade;

  @GET
  @Path("/datasets/{publicDSId}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readme(@PathParam("publicDSId") String publicDSId, @Context SecurityContext sc) throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "remote:dela:readme {0}", publicDSId);
    Optional<Dataset> dataset = datasetFacade.findByPublicDsId(publicDSId);
    if (!dataset.isPresent() || !dataset.get().isPublicDs()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DATASET_DOES_NOT_EXIST, Level.FINE,
        DelaException.Source.REMOTE_DELA);
    }
    FilePreviewDTO result = hdfsDelaCtrl.getPublicReadme(dataset.get());
    LOGGER.log(Settings.DELA_DEBUG, "remote:dela:readme - done {0}", publicDSId);
    return success(result);
  }

  private Response success(Object content) {
    return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.OK).entity(content).build();
  }
}
