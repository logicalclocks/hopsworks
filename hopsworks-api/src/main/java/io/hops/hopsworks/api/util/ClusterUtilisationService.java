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

package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.util.ClusterUtil;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("/clusterUtilisation")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Cluster Utilisation Service",
    description = "Cluster Utilisation Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterUtilisationService {

  private final static Logger LOGGER = Logger.getLogger(
      ClusterUtilisationService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ClusterUtil clusterUtil;
  @EJB
  private Settings settings;

  @GET
  @Path("/metrics")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGpus() {
    Response response = null;
    String rmUrl = "http://" + settings.getRmIp() + ":" + settings.getRmPort() + "/ws/v1/cluster/metrics";
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(rmUrl);
    try {
      response = target.request().get();
    } catch (Exception ex) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.SERVICE_UNAVAILABLE).build();
    } finally {
      client.close();
    }
    return response;
  }

}
