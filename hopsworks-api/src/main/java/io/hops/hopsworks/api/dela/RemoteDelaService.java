/*
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
 *
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
