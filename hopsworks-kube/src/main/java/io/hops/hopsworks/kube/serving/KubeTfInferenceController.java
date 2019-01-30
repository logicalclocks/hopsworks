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
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.netty.channel.ConnectTimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.Singleton;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

@Alternative
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeTfInferenceController implements TfInferenceController {

  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;

  private static final Logger logger = Logger.getLogger(KubeTfInferenceController.class.getName());

  private List<String> nodeIPLists;
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
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
          e.getMessage(), e);
    }

    if (serviceInfo == null) {
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
    HttpEntity httpEntity = null;
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
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    try {
      int nRetry = 3;
      while (nRetry > 0) {
        try {
          HttpPost request = new HttpPost(uri);
          request.addHeader("content-type", "application/json; charset=utf-8");
          request.setEntity(new StringEntity(inferenceRequestJson));
          HttpContext context = HttpClientContext.create();
          response = httpClient.execute(request, context);
          httpEntity = response.getEntity();
          if (httpEntity == null) {
            throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTY_RESPONSE, Level.INFO,
              "Received null response");
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
        } catch (ConnectTimeoutException e) {
          // Maybe the node we are trying to send requests to died. Refresh the list.
          refreshNodeList();
        } finally {
          nRetry--;
        }
      }
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO, "Failed to get a response " +
        "from serving server");
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

  @PreDestroy
  public void preDestroy() {
    if(cm != null) {
      cm.close();
    }
  }
}
