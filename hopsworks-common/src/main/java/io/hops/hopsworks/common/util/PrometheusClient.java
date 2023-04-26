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
package io.hops.hopsworks.common.util;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.client.HttpRetryableAction;
import io.hops.hopsworks.common.proxies.client.NotRetryableClientProtocolException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import org.apache.hadoop.util.ExponentialBackOff;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;

import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("Settings")
public class PrometheusClient {
  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private PoolingHttpClientConnectionManager connectionManager;
  private CloseableHttpClient client;
  private ExponentialBackOff.Builder backOffPolicy;

  private String prometheusIP = "";

  @PostConstruct
  public void init() throws RuntimeException {
    try {
      Service prometheusService =
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.PROMETHEUS.getName());
      prometheusIP = prometheusService.getAddress();
      connectionManager = createConnectionManager();
      client = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();
    } catch (IOException | GeneralSecurityException | ServiceDiscoveryException ex) {
      if (!(ex instanceof ServiceDiscoveryException)) {
        throw new RuntimeException(ex);
      }
    }
    backOffPolicy = new ExponentialBackOff.Builder()
        .setMaximumRetries(10)
        .setInitialIntervalMillis(500)
        .setMaximumIntervalMillis(3000)
        .setMultiplier(1.5);
  }

  @PreDestroy
  public void destroy() {
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
  }

  private PoolingHttpClientConnectionManager createConnectionManager() throws IOException, GeneralSecurityException {
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(10);
    connectionManager.setDefaultMaxPerRoute(10);
    return connectionManager;
  }

  public JSONObject execute(String query) throws ServiceException {
    try {
      if (Strings.isNullOrEmpty(prometheusIP)) {
        Service prometheusService =
            serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
                HopsworksService.PROMETHEUS.getName());
        prometheusIP = prometheusService.getAddress();
      }
      final HttpUriRequest httpRequest = new HttpGet(getUri(query));
      httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      HttpRetryableAction<JSONObject> retryableAction = new HttpRetryableAction<JSONObject>(backOffPolicy) {
        @Override
        public JSONObject performAction() throws ClientProtocolException, IOException {
          return client.execute(httpRequest, httpResponse -> {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode / 100 == 2) {
              String response = EntityUtils.toString(httpResponse.getEntity());
              return Strings.isNullOrEmpty(response) ? new JSONObject() : new JSONObject(response);
            } else if (statusCode / 100 == 4) {
              if (statusCode == 404) {
                //Retry
                throw new ClientProtocolException();
              } else {
                throw new NotRetryableClientProtocolException(httpResponse.toString());
              }
            } else {
              // Retry
              throw new ClientProtocolException();
            }
          });
        }
      };
      return retryableAction.tryAction();
    } catch (IOException | URISyntaxException | ServiceDiscoveryException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.PROMETHEUS_QUERY_ERROR, Level.FINE, "Failed to execute " +
          "prometheus query " + query, e.getMessage());
    }
  }

  private String getUri(String query) throws MalformedURLException, URISyntaxException {
    HttpHost rmHost = new HttpHost(prometheusIP, settings.getPrometheusPort(), "http");
    URL url= new URL(rmHost.toURI() + "/api/v1/query?query=" + query);
    URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
        url.getQuery(), url.getRef());
    return uri.toASCIIString();
  }
}
