/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.PrometheusClient;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Logged
@Stateless
@Path("/prometheusQuery")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Prometheus Query", description = "Prometheus Query Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PrometheusQueryResource {
  private final static Logger LOGGER = Logger.getLogger(PrometheusQueryResource.class.getName());
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private HttpClient httpClient;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private PrometheusClient prometheusClient;

  @GET
  @Logged(logLevel = LogLevel.OFF)
  @Produces(MediaType.APPLICATION_JSON)
  public Response executePrometheusQuery(@QueryParam("query") String query) throws ServiceException {
    JSONObject jsonObject = prometheusClient.execute(query);
    return Response.ok()
        .entity(jsonObject.toString())
        .build();
  }
}
