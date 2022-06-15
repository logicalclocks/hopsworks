/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.serving.inference.ServingInferenceController;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeInferenceEndpoints;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Kube Inference Controller
 *
 * Sends inference requests to a local tensorflow serving server to get a prediction response
 */
@KubeStereotype
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeInferenceController implements ServingInferenceController {

  @EJB
  private KubeKServeInferenceController kubeKServeInferenceController;
  @EJB
  private KubeDeploymentInferenceController kubeDeploymentInferenceController;
  @EJB
  private KubeInferenceEndpoints kubeInferenceEndpoints;
  
  /**
   * Kube inference. Sends a JSON request to the REST API of a kube serving server
   *
   * @param serving the serving instance to send the request to
   * @param modelVersion the version of the serving
   * @param verb the type of inference request (predict, regress, classify)
   * @param inferenceRequestJson the JSON payload of the inference request
   * @param authHeader the Authorization header of the request
   * @return the inference result returned by the serving server
   * @throws InferenceException
   */
  public Pair<Integer, String> infer(String username, Serving serving, Integer modelVersion, InferenceVerb verb,
    String inferenceRequestJson, String authHeader) throws InferenceException, ApiKeyException {
  
    // Check verb
    if (verb == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.MISSING_VERB, Level.FINE);
    }
    
    // KServe
    if (serving.getServingTool() == ServingTool.KSERVE) {
      return kubeKServeInferenceController.infer(username, serving, verb, inferenceRequestJson, authHeader);
    }
    // Default
    return kubeDeploymentInferenceController.infer(serving, verb, inferenceRequestJson);
  }
  
  public List<InferenceEndpoint> getInferenceEndpoints() {
    InferenceEndpoint nodeEndpoint =
      kubeInferenceEndpoints.getEndpoint(InferenceEndpoint.InferenceEndpointType.NODE);
    InferenceEndpoint loadBalancerEndpoint =
      kubeInferenceEndpoints.getEndpoint(InferenceEndpoint.InferenceEndpointType.LOAD_BALANCER);
    return new ArrayList<InferenceEndpoint>(){
      {
        if (nodeEndpoint != null) {
          add(nodeEndpoint);
        }
        if (loadBalancerEndpoint != null) {
          add(loadBalancerEndpoint);
        }
      }
    };
  }
}
