/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Utils for creating deployments for Scikit-learn models on Kubernetes.
 *
 * It implements methods for KFServing deployments and reuses existing utils methods for default deployments.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubePredictorPythonSklearnUtils extends KubePredictorServerUtils {
  
  @EJB
  private KubePredictorPythonUtils kubePredictorPythonUtils;
  @EJB
  private KubeJsonUtils kubeJsonUtils;
  
  // Default
  
  @Override
  public String getDeploymentName(String servingId) { return kubePredictorPythonUtils.getDeploymentName(servingId); }
  
  @Override
  public String getDeploymentPath(String servingName, Integer modelVersion, InferenceVerb verb) {
    return kubePredictorPythonUtils.getDeploymentPath(verb);
  }
  
  @Override
  public String getServiceName(String servingId) { return kubePredictorPythonUtils.getServiceName(servingId); }
  
  @Override
  public Deployment buildServingDeployment(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException {
    return kubePredictorPythonUtils.buildDeployment(project, user, serving);
  }
  
  @Override
  public Service buildServingService(Serving serving) { return kubePredictorPythonUtils.buildService(serving); }
  
  // KFServing
  
  @Override
  public JSONObject buildInferenceServicePredictor(Project project, Users user, Serving serving, String artifactPath) {
    
    // Inference logging
    InferenceLogging inferenceLogging = serving.getInferenceLogging();
    boolean logging = inferenceLogging != null;
    String loggingMode;
    if (logging) {
      if (inferenceLogging == InferenceLogging.ALL) {
        loggingMode = KubeServingUtils.INFERENCE_LOGGER_MODE_ALL;
      } else if (inferenceLogging == InferenceLogging.PREDICTIONS) {
        loggingMode = KubeServingUtils.INFERENCE_LOGGER_MODE_RESPONSE;
      } else {
        loggingMode = KubeServingUtils.INFERENCE_LOGGER_MODE_REQUEST;
      }
    } else {
      loggingMode = null;
    }
    
    return kubeJsonUtils.buildPredictorSklearn(artifactPath, serving.getPredictorResources(),
      serving.getInstances(), logging, loggingMode);
  }
}
