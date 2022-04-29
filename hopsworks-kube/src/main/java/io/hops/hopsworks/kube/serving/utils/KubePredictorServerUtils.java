/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;

/**
 * Utils for creating deployments on Kubernetes.
 *
 * It defines methods for both, default and KServe deployments. Implementations for the different types of
 * deployments on Kubernetes extend this class.
 */
public abstract class KubePredictorServerUtils {
  
  public final static String SERVING_ID = "SERVING_ID";
  public final static String MODEL_NAME = "MODEL_NAME";
  
  // Default
  
  public abstract String getDeploymentName(String servingId);
  public abstract String getDeploymentPath(String servingName, Integer modelVersion, InferenceVerb verb);
  public abstract Deployment buildServingDeployment(Project project, Users user, Serving serving) throws
    ServiceDiscoveryException;
  
  public abstract String getServiceName(String servingId);
  public abstract Service buildServingService(Serving serving);
  
  // KServe
  
  public abstract JSONObject buildInferenceServicePredictor(Project project, Users user, Serving serving,
    String artifactPath) throws ServiceDiscoveryException;
}
