/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.inference;

import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.inference.ServingInferenceController;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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
  private KubeKfServingInferenceController kubeKfServingInferenceController;
  @EJB
  private KubeDeploymentInferenceController kubeDeploymentInferenceController;
  
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
  public Pair<Integer, String> infer(Serving serving, Integer modelVersion, String verb, String inferenceRequestJson,
    String authHeader) throws InferenceException {

    // KFServing
    if (serving.getServingTool() == ServingTool.KFSERVING) {
      return kubeKfServingInferenceController.infer(serving, verb, inferenceRequestJson, authHeader);
    }
    // Elastic
    return kubeDeploymentInferenceController.infer(serving, verb, inferenceRequestJson);
  }
}
