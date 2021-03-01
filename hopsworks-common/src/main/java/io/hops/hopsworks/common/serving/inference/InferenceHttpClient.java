/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A HTTP client shared by inference controllers in hopsworks. It is used for relaying Inference requests received in
 * the Hopsworks REST API to the serving servers and then returning the response.
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class InferenceHttpClient {
  
  
  @EJB
  private Settings settings;
  
  private static final Logger logger = Logger.getLogger(InferenceHttpClient.class.getName());
  
  private CloseableHttpClient httpClient = null;
  private PoolingHttpClientConnectionManager cm = null;
  
  /**
   * Initialize the HTTP connection pool configuration
   */
  @PostConstruct
  public void init() {
    cm = new PoolingHttpClientConnectionManager(30l, TimeUnit.SECONDS);
    int poolSize = settings.getServingConnectionPoolSize();
    int maxRouteConnections = settings.getServingMaxRouteConnections();
    cm.setMaxTotal(poolSize);
    cm.setDefaultMaxPerRoute(maxRouteConnections);
    logger.log(Level.FINE, "Creating connection pool for Model Serving of size " +
      poolSize + " and max connections per route " + maxRouteConnections);
    httpClient = HttpClients.custom()
      .setConnectionManager(cm)
      .build();
  }
  
  
  /**
   * Wrapper for sending an inference HTTP request
   *
   * @param request the request to send, containing stuff such as URI, headers etc.
   * @param httpContext the http context
   * @return the http response
   * @throws IOException
   */
  public CloseableHttpResponse execute(HttpPost request, HttpContext httpContext) throws InferenceException {
    try {
      return httpClient.execute(request, httpContext);
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO, null, e.getMessage(), e);
    }
  }
  
  /**
   * Handles a HTTP response to an inference request. Parses the response into a tuple of (statusCode, responseStr)
   *
   * @param response the HTTP response to handle
   * @return a tuple of (statusCode, responseStr)
   * @throws InferenceException in case there was an error handling the response
   */
  public Pair<Integer, String> handleInferenceResponse(CloseableHttpResponse response) throws InferenceException {
    try {
      if (response == null) {
        throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTY_RESPONSE, Level.INFO, "Received null response");
      }
      HttpEntity httpEntity = response.getEntity();
      if (httpEntity == null) {
        throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTY_RESPONSE, Level.INFO, "Received null response");
      }
      try {
        // Return prediction
        String responseStr = EntityUtils.toString(httpEntity);
        EntityUtils.consume(httpEntity);
        return new Pair<>(response.getStatusLine().getStatusCode(), responseStr);
      } catch (IOException e) {
        throw new InferenceException(RESTCodes.InferenceErrorCode.ERROR_READING_RESPONSE, Level.INFO,
          "", e.getMessage(), e);
      }
    } finally {
      try {
        if(response != null) {
          response.close();
        }
      } catch (IOException ioe) {
        logger.log(Level.FINE, "Error closing response" , ioe);
      }
    }
  }
  
  /**
   * Cleanup the http client
   */
  @PreDestroy
  public void preDestroy() {
    if(cm != null) {
      cm.close();
    }
  }
  
}
