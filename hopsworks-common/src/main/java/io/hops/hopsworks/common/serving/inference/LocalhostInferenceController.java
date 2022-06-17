/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.LocalhostServingController.CID_STOPPED;

/**
 * Localhost Inference Controller
 *
 * Sends inference requests to a local serving server to get a prediction response
 */
@LocalhostStereotype
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class LocalhostInferenceController implements ServingInferenceController {
  
  @EJB
  private InferenceHttpClient inferenceHttpClient;
  @EJB
  private LocalhostTfInferenceUtils localhostTfInferenceUtils;
  @EJB
  private LocalhostSkLearnInferenceUtils localhostSkLearnInferenceUtils;
  @EJB
  private ServingInferenceUtils servingInferenceUtils;
  
  /**
   * Local inference. Sends a JSON request to the REST API of a local serving server
   *
   * @param serving the serving instance to send the request to
   * @param modelVersion the version of the serving
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(String username, Serving serving, Integer modelVersion, InferenceVerb verb,
    String inferenceRequestJson, String authHeader) throws InferenceException {
  
    if (serving.getCid().equals(CID_STOPPED)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
    }
    
    String path = getInferencePath(serving, modelVersion, verb);
    
    InferenceEndpoint endpoint = getNodeInferenceEndpoint();
    String host = endpoint.getAnyHost();
    Integer port = serving.getLocalPort(); // use serving local port
    
    try {
      HttpPost request = servingInferenceUtils.buildInferenceRequest(host, port, path, inferenceRequestJson);
      HttpContext context = HttpClientContext.create();
      CloseableHttpResponse response = inferenceHttpClient.execute(request, context);
      return inferenceHttpClient.handleInferenceResponse(response);
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    } catch (UnsupportedCharsetException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.BAD_REQUEST, Level.INFO, null, e.getMessage(), e);
    }
  }
  
  public List<InferenceEndpoint> getInferenceEndpoints() {
    InferenceEndpoint endpoint = getNodeInferenceEndpoint();
    return Collections.singletonList(endpoint);
  }
  
  private InferenceEndpoint getNodeInferenceEndpoint() {
    List<String> hosts = Collections.singletonList("localhost");
    List<InferencePort> ports = Collections.singletonList(new InferencePort(InferencePort.InferencePortName.HTTP,
      null));
    return new InferenceEndpoint(InferenceEndpoint.InferenceEndpointType.NODE, hosts, ports);
  }
  
  private String getInferencePath(Serving serving, Integer modelVersion, InferenceVerb verb) {
    if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      return localhostTfInferenceUtils.getPath(serving.getName(), modelVersion, verb);
    } else if (serving.getModelServer() == ModelServer.PYTHON) {
      return localhostSkLearnInferenceUtils.getPath(verb);
    } else {
      throw new UnsupportedOperationException("Model server not supported as local serving");
    }
  }
}
