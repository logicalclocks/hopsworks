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
