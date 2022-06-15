/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.inference;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceHttpClient;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.serving.inference.ServingInferenceUtils;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeInferenceEndpoints;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URISyntaxException;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeDeploymentInferenceController {
  
  @EJB
  private InferenceHttpClient inferenceHttpClient;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeInferenceEndpoints kubeInferenceEndpoints;
  @EJB
  private ServingInferenceUtils servingInferenceUtils;
  
  /**
   * Kube deployment inference. Sends a JSON request to the REST API of a kube deployment server
   *
   * @param serving the serving instance to send the request to
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(Serving serving, InferenceVerb verb, String inferenceRequestJson)
    throws InferenceException {
    
    KubePredictorServerUtils predictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    String serviceName = predictorServerUtils.getServiceName(serving.getId().toString());
    
    // Get any node host
    InferenceEndpoint endpoint = kubeInferenceEndpoints.getEndpoint(InferenceEndpoint.InferenceEndpointType.NODE);
    String host = endpoint.getAnyHost();
    if (host == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.ENDPOINT_NOT_FOUND, Level.SEVERE);
    }

    // Get node port
    Integer port;
    try {
      Service serviceInfo = kubeClientService.getServiceInfo(serving.getProject(), serviceName);
      if (serviceInfo == null) {
        throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
      }
      port = serviceInfo.getSpec().getPorts().get(0).getNodePort(); // Deployment service nodePort
    } catch (KubernetesClientException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
          e.getMessage(), e);
    }
  
    // Build request
    HttpPost request;
    try {
      request = servingInferenceUtils.buildInferenceRequest(host, port,
        kubeServingUtils.getModelServerInferencePath(serving, verb), inferenceRequestJson);
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    int nRetry = 3;
    while (nRetry > 0) {
      try {
        HttpContext context = HttpClientContext.create();
        CloseableHttpResponse response = inferenceHttpClient.execute(request, context);
        return inferenceHttpClient.handleInferenceResponse(response);
      } catch (InferenceException e) {
        // Maybe the node we are trying to send requests to died.
      } finally {
        nRetry--;
      }
    }

    throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO);
  }
}
