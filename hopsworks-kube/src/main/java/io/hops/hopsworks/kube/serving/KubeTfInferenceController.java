/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.serving.inference.InferenceException;
import io.hops.hopsworks.common.serving.inference.TfInferenceController;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.netty.channel.ConnectTimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Alternative
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeTfInferenceController implements TfInferenceController {

  @EJB
  private KubeClientService kubeClientService;

  private List<String> nodeIPLists;
  private HttpClient httpClient = null;

  @PostConstruct
  public void init() {
    httpClient = HttpClientBuilder.create().build();
    refreshNodeList();
  }

  @Override
  public Pair<Integer, String> infer(TfServing tfServing, Integer modelVersion,
                                     String verb, String inferenceRequestJson) throws InferenceException {

    // Get node port
    Service serviceInfo = null;
    try {
      serviceInfo = kubeClientService.getServiceInfo(tfServing.getProject(),
          KubeTfServingController.getServiceName(tfServing.getId().toString()));
    } catch (KubernetesClientException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGINSTANCEINTERNAL, Level.SEVERE, null,
          e.getMessage(), e);
    }

    if (serviceInfo == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGNOTRUNNING, Level.FINE);
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


    HttpResponse response = null;
    // Send request
    URI uri = null;
    try {
      uri = new URIBuilder()
          .setScheme("http")
          .setHost(nodeIPLists.get(0))
          .setPort(serviceInfo.getSpec().getPorts().get(0).getNodePort())
          .setPath(pathBuilder.toString())
          .build();


    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUESTERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    int nRetry = 3;
    while (nRetry > 0) {
      try {
        HttpPost request = new HttpPost(uri);
        request.addHeader("content-type", "application/json; charset=utf-8");
        request.setEntity(new StringEntity(inferenceRequestJson));
        response = httpClient.execute(request);
        break;
      } catch (ConnectTimeoutException e) {
        // Maybe the node we are trying to send requests to died. Refresh the list.
        refreshNodeList();
        nRetry--;
      } catch (IOException e) {
        throw new InferenceException(RESTCodes.InferenceErrorCode.REQUESTERROR, Level.INFO, null, e.getMessage(), e);
      }
    }

    // Handle response
    if (response == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTYRESPONSE, Level.INFO, "Received null response");
    }

    HttpEntity httpEntity = response.getEntity();
    if (httpEntity == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTYRESPONSE, Level.INFO, "Received null response");
    }

    try {
      // Return prediction
      String responseStr = EntityUtils.toString(httpEntity);
      return new Pair<>(response.getStatusLine().getStatusCode(), responseStr);
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.ERRORREADINGRESPONSE, Level.INFO,
          "", e.getMessage(), e);
    }
  }

  private void refreshNodeList() {
    // Extract node IPs
    nodeIPLists = kubeClientService.getNodeList().stream()
        .flatMap(node -> node.getStatus().getAddresses().stream())
        .filter(nodeAddress -> nodeAddress.getType().equals("InternalIP"))
        .flatMap(nodeAddress -> Stream.of(nodeAddress.getAddress()))
        .collect(Collectors.toList());

    // Shuffle the list so each EJB sends request to a different node
    Collections.shuffle(nodeIPLists);
  }
}
