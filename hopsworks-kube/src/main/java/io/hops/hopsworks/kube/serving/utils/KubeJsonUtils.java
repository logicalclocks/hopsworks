/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeKServeClientService;
import io.hops.hopsworks.persistence.entity.serving.BatchingConfiguration;
import io.hops.hopsworks.persistence.entity.serving.DeployableComponentResources;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeJsonUtils {
  
  private static final Logger LOGGER = Logger.getLogger(KubeJsonUtils.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeServingUtils kubeServingUtils;
  
  // Inference service
  
  public JSONObject buildInferenceService(JSONObject predictor, JSONObject metadata, String resourceVersion,
    JSONObject transformer) {
    JSONObject pipeline = new JSONObject();
    
    // Predictor - node selectors
    JSONObject nodeSelector = null;
    Map<String, String> nodeSelectorLabels = kubeServingUtils.getServingNodeLabels();
    if (nodeSelectorLabels != null) {
      nodeSelector = new JSONObject(nodeSelectorLabels);
      predictor.put("nodeSelector", nodeSelector);
    }
    
    // Predictor - node tolerations
    JSONArray tolerations = null;
    List<Map<String, String>> nodeTolerations = kubeServingUtils.getServingNodeTolerations();
    if (nodeTolerations != null) {
      tolerations = new JSONArray(nodeTolerations.stream().map(JSONObject::new).collect(Collectors.toList()));
      predictor.put("tolerations", tolerations);
    }
    
    pipeline.put("predictor", predictor);
    
    // Transformer - node selectors / tolerations
    if (transformer != null) {
      if (nodeSelector != null) {
        transformer.put("nodeSelector", nodeSelector);
      }
      if (tolerations != null) {
        transformer.put("tolerations", tolerations);
      }
      pipeline.put("transformer", transformer);
    }
    
    // Resource version
    if (resourceVersion != null) {
      metadata.put("resourceVersion", resourceVersion);
    }
    
    return new JSONObject() {
      {
        put("apiVersion", String.format("%s/%s", KubeKServeClientService.INFERENCESERVICE_GROUP,
          KubeKServeClientService.INFERENCESERVICE_VERSION));
        put("kind", KubeKServeClientService.INFERENCESERVICE_KIND);
        put("metadata", metadata);
        put("spec", pipeline);
      }
    };
  }
  
  public JSONObject buildInferenceServiceMetadata(ObjectMeta metadata) {
    return new JSONObject() {
      {
        put("name", metadata.getName());
        put("labels", new JSONObject() {
          {
            for (Map.Entry<String, String> label : metadata.getLabels().entrySet()) {
              put(label.getKey(), label.getValue());
            }
          }
        });
        put("annotations", new JSONObject() {
          {
            for (Map.Entry<String, String> annotation : metadata.getAnnotations().entrySet()) {
              put(annotation.getKey(), annotation.getValue());
            }
          }
        });
      }
    };
  }
  
  // Predictors
  
  public JSONObject buildPredictorTensorflow(String artifactPath, DeployableComponentResources resources,
    Integer minReplicas, boolean logging, String loggingMode, BatchingConfiguration batchingConfiguration) {
    
    JSONObject inferenceService = buildPredictorBase(minReplicas, logging, loggingMode, batchingConfiguration);
    inferenceService.put("tensorflow", buildFrameworkTensorflow(artifactPath, resources));
    
    return inferenceService;
  }
  
  public JSONObject buildPredictorSklearn(String artifactPath, DeployableComponentResources resources,
    Integer minReplicas, boolean logging, String loggingMode, BatchingConfiguration batchingConfiguration) {
    
    JSONObject inferenceService = buildPredictorBase(minReplicas, logging, loggingMode, batchingConfiguration);
    inferenceService.put("sklearn", buildFrameworkSklearn(artifactPath, resources));
    
    return inferenceService;
  }
  
  public JSONObject buildPredictor(List<Container> containers, List<Volume> volumes, Integer minReplicas,
                                   BatchingConfiguration batchingConfiguration) {
    return buildPredictor(containers, volumes, minReplicas, false, null, batchingConfiguration);
  }
  
  public JSONObject buildPredictor(List<Container> containers, List<Volume> volumes, Integer minReplicas,
    boolean logging, String loggingMode, BatchingConfiguration batchingConfiguration) {
    
    // Containers
    JSONArray containersJSON = new JSONArray();
    for (Container container : containers) {
      containersJSON.put(buildContainer(container));
    }
    
    // Infernce service
    JSONObject inferenceService = buildPredictorBase(minReplicas, logging, loggingMode, batchingConfiguration);
    inferenceService.put("containers", containersJSON);
    inferenceService.put("volumes", new JSONArray(volumes));
    
    return inferenceService;
  }
  
  private JSONObject buildPredictorBase(Integer minReplicas, boolean logging, String loggingMode,
                                        BatchingConfiguration batchingConfiguration) {
    return new JSONObject() {
      {
        put("minReplicas", minReplicas);
        put("logger", !logging ? null : new JSONObject() {
          {
            put("mode", loggingMode);
            put("url", String.format("http://%s:%s", KubeServingUtils.INFERENCE_LOGGER_HOST,
              KubeServingUtils.INFERENCE_LOGGER_PORT));
          }
        });
        if (batchingConfiguration.isBatchingEnabled()) {
          put("batcher", new JSONObject() {
            {
              put("maxBatchSize", batchingConfiguration.getMaxBatchSize());
              put("maxLatency", batchingConfiguration.getMaxLatency());
              put("timeout", batchingConfiguration.getTimeout());
            }
          });
        }
      }
    };
  }
  
  // Frameworks predictors
  
  private JSONObject buildFrameworkTensorflow(String artifactPath, DeployableComponentResources resources) {
    
    ResourceRequirements resourceRequirements = kubeClientService.buildResourceRequirements(resources.getLimits(),
      resources.getRequests());
    
    // Tensorflow runtime
    String tensorflowVersion = settings.getTensorflowVersion();
    if (resourceRequirements.getRequests().containsKey("nvidia.com/gpu") ||
      resourceRequirements.getLimits().containsKey("nvidia.com/gpu")) {
      tensorflowVersion += "-gpu";
    }
    String runtimeVersion = tensorflowVersion;
    
    JSONObject predictor = buildFrameworkBase(artifactPath, resourceRequirements);
    predictor.put("runtimeVersion", runtimeVersion);
    
    return predictor;
  }
  
  private JSONObject buildFrameworkSklearn(String artifactPath, DeployableComponentResources resources) {
    return buildFrameworkBase(artifactPath, kubeClientService.buildResourceRequirements(resources.getLimits(),
      resources.getRequests()));
  }
  
  private JSONObject buildFrameworkBase(String artifactPath,
    ResourceRequirements resourceRequirements) {
    
    // Resources
    JSONObject resources = new JSONObject() {
      {
        put("requests", new JSONObject() {
          {
            put("memory", resourceRequirements.getRequests().get("memory"));
            put("cpu", resourceRequirements.getRequests().get("cpu"));
            if (resourceRequirements.getRequests().containsKey("nvidia.com/gpu")) {
              put("nvidia.com/gpu", resourceRequirements.getRequests().get("nvidia.com/gpu"));
            }
          }
        });
        put("limits", new JSONObject() {
          {
            put("memory", resourceRequirements.getLimits().get("memory"));
            put("cpu", resourceRequirements.getLimits().get("cpu"));
            if (resourceRequirements.getLimits().containsKey("nvidia.com/gpu")) {
              put("nvidia.com/gpu", resourceRequirements.getLimits().get("nvidia.com/gpu"));
            }
          }
        });
      }
    };
    
    return new JSONObject() {
      {
        put("storageUri", artifactPath);
        put("resources", resources);
      }
    };
  }
  
  // Container
  
  private JSONObject buildContainer(Container container) {
    JSONObject containerJSON = new JSONObject(container);
    JSONObject resources = containerJSON.getJSONObject("resources");
    buildContainerResources(resources.getJSONObject("requests"), container.getResources().getRequests());
    buildContainerResources(resources.getJSONObject("limits"), container.getResources().getLimits());
    return containerJSON;
  }
  
  private void buildContainerResources(JSONObject resourcesJSON, Map<String, Quantity> resources) {
    resourcesJSON.put("memory", resources.get("memory"));
    resourcesJSON.put("cpu", resources.get("cpu"));
    if (resources.containsKey("nvidia.com/gpu")) {
      resourcesJSON.put("nvidia.com/gpu", resources.get("nvidia.com/gpu"));
    }
  }
}
