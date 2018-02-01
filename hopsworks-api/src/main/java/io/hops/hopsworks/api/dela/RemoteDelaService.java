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

package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaHdfsController;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.swagger.annotations.Api;
import java.util.Optional;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/remote/dela")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Cross Dela Service",
  description = "Cross Dela Service")
public class RemoteDelaService {

  private final static Logger LOG = Logger.getLogger(RemoteDelaService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private DelaHdfsController hdfsDelaCtrl;
  @EJB
  private DatasetFacade datasetFacade;

  @GET
  @Path("/datasets/{publicDSId}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readme(@PathParam("publicDSId") String publicDSId) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "remote:dela:readme {0}", publicDSId);
    Optional<Dataset> dataset = datasetFacade.findByPublicDsId(publicDSId);
    if (!dataset.isPresent() || !dataset.get().isPublicDs()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(),
        ThirdPartyException.Error.DATASET_DOES_NOT_EXIST.toString(), ThirdPartyException.Source.REMOTE_DELA,
        "bad request");
    }
    FilePreviewDTO result = hdfsDelaCtrl.getPublicReadme(dataset.get());
    LOG.log(Settings.DELA_DEBUG, "remote:dela:readme - done {0}", publicDSId);
    return success(result);
  }

  private Response success(Object content) {
    return noCacheResponse.getNoCacheCORSResponseBuilder(Response.Status.OK).entity(content).build();
  }
}
