/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.inference;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.inference.InferenceHttpClient;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
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
  
  /**
   * Kube deployment inference. Sends a JSON request to the REST API of a kube deployment server
   *
   * @param serving the serving instance to send the request to
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(Serving serving, String verb, String inferenceRequestJson)
    throws InferenceException {
    
    // Check verb
    if (Strings.isNullOrEmpty(verb)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.MISSING_VERB, Level.FINE);
    }
    
    KubePredictorServerUtils predictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    String serviceName = predictorServerUtils.getServiceName(serving.getId().toString());
    
    // Get node port
    Service serviceInfo;
    try {
      serviceInfo = kubeClientService.getServiceInfo(serving.getProject(), serviceName);
    } catch (KubernetesClientException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
          e.getMessage(), e);
    }

    if (serviceInfo == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE,
        String.format("Serving instance with name '%s' not running", serving.getName()));
    }

    // Send request
    URI uri;
    try {
      uri = new URIBuilder()
          .setScheme("http")
          .setHost(kubeClientService.getRandomReadyNodeIp())
          .setPort(serviceInfo.getSpec().getPorts().get(0).getNodePort())
          .setPath(kubeServingUtils.getInternalInferencePath(serving, verb))
          .build();

    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    int nRetry = 3;
    while (nRetry > 0) {
      try {
        HttpPost request = new HttpPost(uri);
        request.addHeader("content-type", "application/json; charset=utf-8");
        request.setEntity(new StringEntity(inferenceRequestJson));
        HttpContext context = HttpClientContext.create();
        CloseableHttpResponse response = inferenceHttpClient.execute(request, context);
        return inferenceHttpClient.handleInferenceResponse(response);
      } catch (IOException e) {
        // Maybe the node we are trying to send requests to died.
      } finally {
        nRetry--;
      }
    }

    throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO);
  }
}
