/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.inference.InferenceHttpClient;
import io.hops.hopsworks.common.serving.inference.SkLearnInferenceController;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.serving.Serving;
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

@KubeStereotype
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeSkLearnInferenceController implements SkLearnInferenceController {

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeSkLearnServingController kubeSKLearnServingController;
  @EJB
  private InferenceHttpClient inferenceHttpClient;

  @Override
  public Pair<Integer, String> infer(Serving serving, Integer modelVersion, String verb, String inferenceRequestJson)
      throws InferenceException {
    // Get node port
    Service serviceInfo = null;
    try {
      serviceInfo = kubeClientService.getServiceInfo(serving.getProject(),
          kubeSKLearnServingController.getServiceName(serving.getId().toString()));
    } catch (KubernetesClientException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
          e.getMessage(), e);
    }

    if (serviceInfo == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
    }

    StringBuilder pathBuilder =
        new StringBuilder().append("/").append(verb.replaceFirst(":", ""));

    // Send request
    URI uri = null;
    try {
      uri = new URIBuilder()
          .setScheme("http")
          .setHost(kubeClientService.getRandomReadyNodeIp())
          .setPort(serviceInfo.getSpec().getPorts().get(0).getNodePort())
          .setPath(pathBuilder.toString())
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
