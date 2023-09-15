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

package io.hops.hopsworks.api.util;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.httpclient.HttpConnectionManagerBuilder;
import io.hops.hopsworks.common.pythonresources.PythonResourcesController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.ResourceManagerTags;
import io.swagger.annotations.Api;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Logged
@Stateless
@Path("/clusterUtilisation")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Cluster Utilisation Service", description = "Cluster Utilisation Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterUtilisationService {
  
  private final static Logger LOGGER = Logger.getLogger(ClusterUtilisationService.class.getName());
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private PythonResourcesController pythonResourcesController;
  @EJB
  private BaseHadoopClientsService baseHadoopClientsService;

  private static final String METRICS_ENDPOINT = "/ws/v1/cluster/metrics";

  private CloseableHttpClient httpClient = null;
  private PoolingHttpClientConnectionManager connectionManager = null;

  @Logged(logLevel = LogLevel.OFF)
  @PostConstruct
  public void init() throws RuntimeException {
    try {
      HttpConnectionManagerBuilder connectionBuilder = new HttpConnectionManagerBuilder()
          .withKeyStore(Paths.get(baseHadoopClientsService.getSuperKeystorePath()),
              baseHadoopClientsService.getSuperKeystorePassword().toCharArray(),
              baseHadoopClientsService.getSuperKeystorePassword().toCharArray())
          .withTrustStore(Paths.get(baseHadoopClientsService.getSuperTrustStorePath()),
              baseHadoopClientsService.getSuperTrustStorePassword().toCharArray());

      connectionManager =
          new PoolingHttpClientConnectionManager(connectionBuilder.build());
      connectionManager.setMaxTotal(10);
      connectionManager.setDefaultMaxPerRoute(10);
      httpClient = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Failed to create HTTP client with superuser client certificate", ex);
      throw new RuntimeException(ex);
    }
  }

  @Logged(logLevel = LogLevel.OFF)
  @PreDestroy
  public void destroy() {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (Exception e) {
      }
    }
    if (connectionManager != null) {
      connectionManager.close();
    }
  }

  @GET
  @Path("/metrics")
  @Logged(logLevel = LogLevel.OFF)
  @Produces(MediaType.APPLICATION_JSON)
  public Response metrics(@Context HttpServletRequest request) throws ServiceException {
    Service rm = null;
    try {
      rm = serviceDiscoveryController
          .getAnyAddressOfServiceWithDNS(
              HopsworksService.RESOURCE_MANAGER.getNameWithTag(ResourceManagerTags.https));
    } catch (ServiceDiscoveryException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_DISCOVERY_ERROR, Level.FINE);
    }

    HttpHost rmHost = new HttpHost(rm.getAddress(), rm.getPort(), "https");
    HttpGet getRequest = new HttpGet(METRICS_ENDPOINT);

    String response = null; // defined as string as we don't really need to look inside it
    try {
      response = httpClient.execute(rmHost, getRequest, new HttpClient.StringResponseHandler());
    } catch (IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.RM_METRICS_ERROR, Level.FINE);
    }
    JSONObject jsonObject = new JSONObject(response);
    jsonObject.put("deploying", hostsFacade.countUnregistered());
    return Response.ok()
      .entity(jsonObject.toString())
      .build();
  }

  @GET
  @Path("/pythonResources")
  @Logged(logLevel = LogLevel.OFF)
  @Produces(MediaType.APPLICATION_JSON)
  public Response pythonResources() throws ServiceDiscoveryException {
    return Response.ok()
        .entity(pythonResourcesController.getPythonResources().toString())
        .build();
  }
}
