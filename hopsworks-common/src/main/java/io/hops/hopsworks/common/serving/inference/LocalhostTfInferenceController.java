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
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.LocalhostServingController.PID_STOPPED;

/**
 * Tensorflow Localhost Inference Controller
 * <p>
 * Sends inference requests to a local tensorflow serving server to get a prediction response
 */
@LocalhostStereotype
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LocalhostTfInferenceController implements TfInferenceController {
  
  @EJB
  private InferenceHttpClient inferenceHttpClient;
  
  /**
   * Tensorflow inference. Sends a JSON request to the REST API of a tensorflow serving server
   *
   * @param serving the tensorflow serving instance to send the request to
   * @param modelVersion the version of the serving
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(Serving serving, Integer modelVersion,
    String verb, String inferenceRequestJson) throws InferenceException {
    
    if (serving.getLocalPid().equals(PID_STOPPED)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
    }
    
    // TODO(Fabio) does Tf model server support TLS?
    StringBuilder pathBuilder = new StringBuilder()
      .append("/v1/models/")
      .append(serving.getName());
    
    // Append the version if the user specified it.
    if (modelVersion != null) {
      pathBuilder.append("/versions").append(modelVersion);
    }
    
    pathBuilder.append(verb);
    
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
      CloseableHttpResponse response = inferenceHttpClient.execute(request, context);
      return inferenceHttpClient.handleInferenceResponse(response);
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO, null, e.getMessage(), e);
    }
  }
}
