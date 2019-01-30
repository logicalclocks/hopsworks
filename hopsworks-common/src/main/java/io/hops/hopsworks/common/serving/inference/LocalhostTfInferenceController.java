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

import com.google.common.base.Strings;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
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

import static io.hops.hopsworks.common.serving.tf.LocalhostTfServingController.PID_STOPPED;

@Alternative
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LocalhostTfInferenceController implements TfInferenceController {

  @EJB
  private Settings settings;

  private static final Logger logger = Logger.getLogger(LocalhostTfInferenceController.class.getName());

  CloseableHttpClient httpClient = null;
  PoolingHttpClientConnectionManager cm = null;

  @PostConstruct
  public void init() {
    cm = new PoolingHttpClientConnectionManager(30l, TimeUnit.SECONDS);
    int poolSize = settings.getTFServingConnectionPoolSize();
    int maxRouteConnections = settings.getTFServingMaxRouteConnections();
    cm.setMaxTotal(poolSize);
    cm.setDefaultMaxPerRoute(maxRouteConnections);
    logger.log(Level.FINE, "Creating connection pool for TF Serving of size " +
      poolSize + " and max connections per route " +
      maxRouteConnections);

    httpClient = HttpClients.custom()
      .setConnectionManager(cm)
      .build();
  }

  public Pair<Integer, String> infer(TfServing tfServing, Integer modelVersion,
                                     String verb, String inferenceRequestJson) throws InferenceException {
    if (tfServing.getLocalPid().equals(PID_STOPPED)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
    }

    if (Strings.isNullOrEmpty(verb)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.MISSING_VERB, Level.FINE);
    }

    // TODO(Fabio) does Tf model server support TLS?

    StringBuilder pathBuilder = new StringBuilder()
        .append("/v1/models/")
        .append(tfServing.getModelName());

    // Append the version if the user specified it.
    if (modelVersion != null) {
      pathBuilder.append("/versions").append(modelVersion);
    }

    pathBuilder.append(verb);

    CloseableHttpResponse response = null;
    // Send request
    try {
      URI uri = new URIBuilder()
          .setScheme("http")
          .setHost("localhost")
          .setPort(tfServing.getLocalPort())
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
