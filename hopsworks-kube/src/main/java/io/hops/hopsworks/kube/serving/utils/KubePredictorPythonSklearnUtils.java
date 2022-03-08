/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.DockerResourcesConfiguration;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubePredictorPythonSklearnUtils extends KubePredictorServerUtils {
  
  @EJB
  private Settings settings;
  @EJB
  private KubePredictorPythonUtils kubePredictorPythonUtils;
  
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
  
  @Override
  public JSONObject buildInferenceServicePredictor(Project project, Users user, Serving serving, String artifactPath) {
    InferenceLogging inferenceLogging = serving.getInferenceLogging();
    
    // Sklearn spec
    JSONObject sklearn = getInferenceServiceSklearn(artifactPath, serving.getDockerResourcesConfig());
  
    // Inference logging
    boolean logging = inferenceLogging != null;
    String loggerMode;
    if (logging) {
      if (inferenceLogging == InferenceLogging.ALL) {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_ALL;
      } else if (inferenceLogging == InferenceLogging.PREDICTIONS) {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_RESPONSE;
      } else {
        loggerMode = KubeServingUtils.INFERENCE_LOGGER_MODE_REQUEST;
      }
    } else {
      loggerMode = null;
    }
    String finalLoggerMode = loggerMode;
  
    // Predictor
    JSONObject inferenceServicePredictorConfig = new JSONObject() {
      {
        put("minReplicas", serving.getInstances());
        put("logger", !logging ? null : new JSONObject() {
          {
            put("mode", finalLoggerMode);
            put("url", String.format("http://%s:%s", KubeServingUtils.INFERENCE_LOGGER_HOST,
              KubeServingUtils.INFERENCE_LOGGER_PORT));
          }
        });
        put("sklearn", sklearn);
      }
    };
  
    return inferenceServicePredictorConfig;
  }
  
  private JSONObject getInferenceServiceSklearn(String artifactPath,
    DockerResourcesConfiguration dockerResourcesConfiguration) {
    
    // Resources configuration
    String memory = dockerResourcesConfiguration.getMemory() + "Mi";
    String cores = Double.toString(dockerResourcesConfiguration.getCores() *
      settings.getKubeDockerCoresFraction());
    JSONObject resources = new JSONObject() {
      {
        put("requests", new JSONObject() {
          {
            put("memory", memory);
            put("cpu", cores);
          }
        });
        put("limits", new JSONObject() {
          {
            put("memory", memory);
            put("cpu", cores);
          }
        });
      }
    };
    
    if (dockerResourcesConfiguration.getGpus() > 0) {
      resources.getJSONObject("limits").put("nvidia.com/gpu", dockerResourcesConfiguration.getGpus());
    }
    
    // Sklearn spec
    return new JSONObject() {
      {
        put("storageUri", artifactPath);
        put("resources", resources);
      }
    };
  }
}
