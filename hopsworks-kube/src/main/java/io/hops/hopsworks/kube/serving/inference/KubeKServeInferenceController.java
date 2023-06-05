/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.inference;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceHttpClient;
import io.hops.hopsworks.common.serving.inference.InferencePort;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.serving.inference.ServingInferenceUtils;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeInferenceEndpoints;
import io.hops.hopsworks.kube.common.KubeKServeClientService;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeKServeInferenceController {
  
  private static final Logger logger = Logger.getLogger(KubeKServeInferenceController.class.getName());
  
  @EJB
  private KubeKServeClientService kubeKServeClientService;
  @EJB
  private KubeInferenceEndpoints kubeInferenceEndpoints;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private InferenceHttpClient inferenceHttpClient;
  @EJB
  private ServingInferenceUtils servingInferenceUtils;
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @EJB
  private UserFacade userFacade;
  
  private final static String TORNADO_ERROR_RESPONSE_PATTERN = "^<html><title>(\\d{3}):(.*?)</title><body>(\\d{3}):(" +
    ".*?)</body></html>$";
  
  /**
   * KServe inference. Sends a JSON request to the REST API of a kserve server
   *
   * @param serving the serving instance to send the request to
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @param authHeader the Authorization header of the request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(String username, Serving serving, InferenceVerb verb, String inferenceRequestJson,
    String authHeader) throws InferenceException, ApiKeyException {
    
    // validate verb
    if (verb == InferenceVerb.TEST) {
      // if header contains JWT token, replace with API Key
      if (authHeader.startsWith(KubeApiKeyUtils.AUTH_HEADER_BEARER_PREFIX)) {
        authHeader = buildApiKeyAuthHeader(username);
        verb = InferenceVerb.PREDICT;
      }
    } else if (verb != InferenceVerb.PREDICT) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.BAD_REQUEST, Level.FINE, String.format("Verb %s not" +
        " supported in KServe deployments", verb.toString()));
    }
    if (!authHeader.startsWith(KubeApiKeyUtils.AUTH_HEADER_API_KEY_PREFIX)) {
      // JWT not supported for KServe (Istio auth)
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_AUTH_TYPE_NOT_SUPPORTED, Level.FINE,
        "Inference requests to KServe require authentication with API Keys");
    }
    
    String inferenceServiceUrl = getInferenceServiceUrl(serving);
    HttpPost request = buildInferenceRequest(serving, inferenceServiceUrl, authHeader, verb, inferenceRequestJson);
    
    // try sending the inference request (3 attempts)
    try {
      Pair<Integer, String> response = sendInferenceRequest(request, 3);
      return response;
    } catch (InferenceException e) {
      if (e.getErrorCode().getCode().intValue() == RESTCodes.InferenceErrorCode.REQUEST_ERROR.getCode().intValue()) {
        // if if fails to reach the node, it can be that it was destroyed by an autoscaler
        // invalidate the inference endpoints cache, and retry
        logger.info("Invalidating inference endpoints cache");
        kubeInferenceEndpoints.invalidateCache();
        request = buildInferenceRequest(serving, inferenceServiceUrl, authHeader, verb, inferenceRequestJson);
      } else {
        throw e;
      }
    }
    
    // retry sending the inference request
    try {
      Pair<Integer, String> response = sendInferenceRequest(request, 2);
      return response;
    } catch (InferenceException e) {
      // resetting the inference endpoints cache didn't work,
      // maybe the node we are trying to send requests to died.
    }
    throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.INFO);
  }
  
  private String buildApiKeyAuthHeader(String username) throws ApiKeyException, InferenceException {
    // get user from db
    Users user = userFacade.findByUsername(username);
    // get API key from kube secret
    Optional<ApiKey> apiKey = kubeApiKeyUtils.getServingApiKey(user);
    if (!apiKey.isPresent()) {
      // serving api key not found
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_AUTH_TYPE_NOT_SUPPORTED, Level.FINE,
        "Inference requests to KServe require authentication with API Keys",
        String.format("Serving API Key not found for user ", user.getUsername()));
    }
    String secretName = kubeApiKeyUtils.getServingApiKeySecretName(apiKey.get().getPrefix());
    String rawSecret = kubeApiKeyUtils.getServingApiKeyValueFromKubeSecret(username, secretName);
    // replace auth header and verb
    return KubeApiKeyUtils.AUTH_HEADER_API_KEY_PREFIX + rawSecret;
  }
  
  private String getInferenceServiceUrl(Serving serving) throws InferenceException {
    // get inference service url
    JSONObject inferenceServiceStatus;
    try {
      Project project = serving.getProject();
      inferenceServiceStatus = kubeKServeClientService.getInferenceServiceStatus(project, serving);
    } catch (KubernetesClientException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
        e.getMessage(), e);
    }
    if (inferenceServiceStatus == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_NOT_RUNNING, Level.FINE);
    }
    return inferenceServiceStatus.getString("url");
  }
  
  private HttpPost buildInferenceRequest(Serving serving, String inferenceServiceURL, String authHeader,
    InferenceVerb verb, String inferenceRequestJson) throws InferenceException {
    // Get host header
    String hostHeader;
    try {
      hostHeader = (new URI(inferenceServiceURL)).getHost();
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVING_INSTANCE_INTERNAL, Level.SEVERE, null,
        e.getMessage(), e);
    }
    
    // Get istio host and port
    InferenceEndpoint endpoint = kubeInferenceEndpoints.getEndpoint(InferenceEndpoint.InferenceEndpointType.NODE);
    String host = endpoint.getAnyHost();
    if (host == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.ENDPOINT_NOT_FOUND, Level.SEVERE);
    }
    Integer port = endpoint.getPort(InferencePort.InferencePortName.HTTP).getNumber();
    
    // Build request
    HttpPost request;
    try {
      request = servingInferenceUtils.buildInferenceRequest(host, port,
        kubeServingUtils.getModelServerInferencePath(serving, verb), inferenceRequestJson);
      request.addHeader("host", hostHeader); // needed by Istio to route the request
      request.addHeader("authorization", authHeader); // istio auth
      return request;
    } catch (URISyntaxException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUEST_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  private Pair<Integer, String> sendInferenceRequest(HttpPost request, Integer numRetry) throws InferenceException {
    InferenceException ex = null;
    while (numRetry > 0) {
      try {
        HttpContext context = HttpClientContext.create();
        CloseableHttpResponse response = inferenceHttpClient.execute(request, context);
        Pair<Integer, String> inferenceResult = inferenceHttpClient.handleInferenceResponse(response);
        return inferenceResult.getL() >= 400 ? parseInferenceErrorResponse(inferenceResult) : inferenceResult;
      } catch (InferenceException e) {
        ex = e;
      } finally {
        numRetry--;
      }
    }
    
    // if all the attempts failed, re-raise exception
    assert ex != null;
    throw ex;
  }
  
  private Pair<Integer, String> parseInferenceErrorResponse(Pair<Integer, String> inferenceResult) {
    String reason = inferenceResult.getR();
    if (reason.startsWith("<html>")) {
      // if it's an error response with an html template, extract the body
      // this mainly occurs in deployments for python models without predictor script (sklearnserver)
      Pattern pattern = Pattern.compile(TORNADO_ERROR_RESPONSE_PATTERN);
      Matcher matcher = pattern.matcher(reason);
      if (matcher.find()) {
        return new Pair<>(inferenceResult.getL(), matcher.group(4));
      } // if unknown html template, return the original message
    } else if (reason.startsWith("{")) {
      // if it's an error response with a json string, extract the error message
      // this mainly occurs in deployments for tensorflow models
      try {
        JSONObject reasonJson = new JSONObject(reason);
        if (reasonJson.has("error")) {
          return new Pair<>(inferenceResult.getL(), reasonJson.getString("error"));
        }
      } catch (JSONException e) {
        // if unknown json template, return the original message
      }
    }
    return inferenceResult;
  }
}
