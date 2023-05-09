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
package io.hops.hopsworks.common.pythonresources;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.proxies.client.NotRetryableClientProtocolException;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.PrometheusTags;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.CLUSTER_TOTAL_CPU_CAPACITY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.DOCKER_TOTAL_ALLOCATABLE_CPU_KEY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.DOCKER_CURRENT_MEMORY_USAGE_KEY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.DOCKER_CURRENT_CPU_USAGE_KEY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.CLUSTER_TOTAL_MEMORY_CAPACITY;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.CLUSTER_CURRENT_MEMORY_USAGE;
import static io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController.CLUSTER_CURRENT_CPU_USAGE;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PythonResourcesController {

  private final static Logger LOGGER = Logger.getLogger(PythonResourcesController.class.getName());

  @EJB
  private HttpClient httpClient;
  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @Inject
  private PythonResourcesQueryController pythonResourcesQueryController;

  private Service prometheusService;

  @PostConstruct
  public void init() {
    try {
      prometheusService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          HopsworksService.PROMETHEUS.getNameWithTag(PrometheusTags.prometheus));
    } catch (ServiceDiscoveryException se) {
      // Nothing to do
    }
  }

  public JSONObject getPythonResources() throws ServiceDiscoveryException {
    JSONObject pythonResources = new JSONObject();

    pythonResourcesQueryController.getPrometheusQueries()
        .forEach((key, query) -> getResourceValue(key, query, pythonResources));

    pythonResources.put(CLUSTER_TOTAL_CPU_CAPACITY, 100);
    if (!settings.isDockerCgroupEnabled() || settings.getKubeInstalled()) {
      //use the same values as the cluster
      pythonResources.put(DOCKER_TOTAL_ALLOCATABLE_CPU_KEY, 100);
      pythonResources.put(DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY, pythonResources.get(CLUSTER_TOTAL_MEMORY_CAPACITY));
      pythonResources.put(DOCKER_CURRENT_MEMORY_USAGE_KEY, pythonResources.get(CLUSTER_CURRENT_MEMORY_USAGE));
      pythonResources.put(DOCKER_CURRENT_CPU_USAGE_KEY, pythonResources.get(CLUSTER_CURRENT_CPU_USAGE));
    }
    return pythonResources;
  }

  private void getResourceValue(String resource, String query, JSONObject pythonResources) {
    try {
      JSONObject queryResult = executeQuery(query);
      JSONArray resultObject = queryResult.getJSONObject("data").getJSONArray("result");
      if (resultObject.length() > 0) {
        pythonResources.put(resource, resultObject.getJSONObject(0).getJSONArray("value").getString(1));
      } else {
        pythonResources.put(resource, "");
      }
    } catch (ServiceException e) {
      pythonResources.put(resource, "");
    }
  }

  public JSONObject executeQuery(String query) throws ServiceException {
    try {
      HttpHost prometheusHost = new HttpHost(prometheusService.getName(), prometheusService.getPort(), "http");
      HttpGet queryRequest = new HttpGet("/api/v1/query?query=" + URLEncoder.encode(query, "UTF-8"));

      return httpClient.execute(prometheusHost, queryRequest, httpResponse -> {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode / 100 == 2) {
          String response = EntityUtils.toString(httpResponse.getEntity());
          return Strings.isNullOrEmpty(response) ? new JSONObject() : new JSONObject(response);
        } else {
          throw new NotRetryableClientProtocolException(httpResponse.toString());
        }
      });
    } catch (IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.PROMETHEUS_QUERY_ERROR, Level.FINE,
          "Failed to execute prometheus query " + query, e.getMessage());
    }
  }
}
