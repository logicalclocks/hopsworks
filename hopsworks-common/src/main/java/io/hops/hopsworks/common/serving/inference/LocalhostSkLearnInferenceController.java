/*
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
 */

package io.hops.hopsworks.common.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SkLearn Localhost Inference Controller
 *
 * Sends inference requests to a local sklearn flask server to get a prediction response
 */
@Alternative
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LocalhostSkLearnInferenceController implements SkLearnInferenceController {

  @EJB
  private Settings settings;

  private static final Logger logger = Logger.getLogger(LocalhostSkLearnInferenceController.class.getName());

  CloseableHttpClient httpClient = null;
  PoolingHttpClientConnectionManager cm = null;
  
  /**
   * Initialize the HTTP connection pool configuration
   */
  @PostConstruct
  public void init() {
    cm = new PoolingHttpClientConnectionManager(30l, TimeUnit.SECONDS);
    int poolSize = settings.getSkLearnConnectionPoolSize();
    int maxRouteConnections = settings.getSkLearnMaxRouteConnections();
    cm.setMaxTotal(poolSize);
    cm.setDefaultMaxPerRoute(maxRouteConnections);
    logger.log(Level.FINE, "Creating connection pool for SkLearn of size " +
      poolSize + " and max connections per route " + maxRouteConnections);

    httpClient = HttpClients.custom()
      .setConnectionManager(cm)
      .build();
  }
  
  /**
   * SkLearn inference. Sends a JSON request to a flask server that serves a SkLearn model
   *
   * @param serving the sklearn serving instance to send the request to
   * @param modelVersion the version of the serving
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(Serving serving, Integer modelVersion,
                                     String verb, String inferenceRequestJson) throws InferenceException {
    

    StringBuilder pathBuilder =
        new StringBuilder().append("/").append(verb.replaceFirst(":", ""));

    CloseableHttpResponse response = null;
    // Send request
    try {
      URI uri = new URIBuilder()
          .setScheme("http")
          .setHost("localhost")
          .setPort(serving.getLocalPort())
          .setPath(pathBuilder.toString())
          .build();
      HttpPost request = new HttpPost(uri);
      request.addHeader("content-type", "application/json; charset=utf-8");
      request.setEntity(new StringEntity(inferenceRequestJson));
      HttpContext context = HttpClientContext.create();
      response = httpClient.execute(request, context);
      // Handle response
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
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO, null, e.getMessage(), e);
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

  @PreDestroy
  public void preDestroy() {
    if(cm != null) {
      cm.close();
    }
  }
}
