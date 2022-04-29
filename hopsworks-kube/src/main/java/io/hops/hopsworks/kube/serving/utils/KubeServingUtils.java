/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeServingUtils {
  
  private final static Integer LAST_REVISION_LENGTH = 8;
  
  @EJB
  private Settings settings;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  
  // Namespaces
  public final static String HOPS_SYSTEM_NAMESPACE = "hops-system";
  
  // Config maps
  public final static String HOPS_SYSTEM_USERS = HOPS_SYSTEM_NAMESPACE + "--users";
  
  // Labels
  public final static String LABEL_PREFIX = "serving.hops.works";
  public final static String SERVING_ID_LABEL_NAME = LABEL_PREFIX + "/id";
  public final static String SERVING_NAME_LABEL_NAME = LABEL_PREFIX + "/name";
  public final static String PROJECT_ID_LABEL_NAME = LABEL_PREFIX + "/project-id";
  public final static String CREATOR_LABEL_NAME = LABEL_PREFIX + "/creator";
  public final static String MODEL_NAME_LABEL_NAME = LABEL_PREFIX + "/model-name";
  public final static String MODEL_VERSION_LABEL_NAME = LABEL_PREFIX + "/model-version";
  public final static String ARTIFACT_VERSION_LABEL_NAME = LABEL_PREFIX + "/artifact-version";
  public final static String MODEL_SERVER_LABEL_NAME = LABEL_PREFIX + "/model-server";
  public final static String SERVING_TOOL_LABEL_NAME = LABEL_PREFIX + "/tool";
  public final static String REVISION_LABEL_NAME = LABEL_PREFIX + "/revision";
  
  public final static String NODE_LABELS_TOLERATIONS_SEPARATOR = ",";
  public final static String NODE_SELECTOR_KEY_VALUE_SEPARATOR = "=";
  public final static String NODE_TOLERATIONS_ATTR_SEPARATOR = ":";
  
  public final static String COMPONENT_LABEL_NAME = "component";
  
  public final static String RESERVED_LABEL_NAME = KubeServingUtils.LABEL_PREFIX + "/reserved";
  public final static String SCOPE_LABEL_NAME = KubeServingUtils.LABEL_PREFIX + "/scope";
  public final static String SCOPE_SERVING_LABEL_VALUE = "serving";
  
  // Annotations
  public final static String ARTIFACT_PATH_ANNOTATION_NAME = LABEL_PREFIX + "/artifact-path";
  public final static String TRANSFORMER_ANNOTATION_NAME = LABEL_PREFIX + "/transformer";
  public final static String PREDICTOR_ANNOTATION_NAME = LABEL_PREFIX + "/predictor";
  public final static String TOPIC_NAME_ANNOTATION_NAME = LABEL_PREFIX + "/topic-name";
  
  // Inference logger
  public final static String INFERENCE_LOGGER_HOST = "localhost";
  public final static Integer INFERENCE_LOGGER_PORT = 9099;
  public final static String INFERENCE_LOGGER_MODE_ALL = "all";
  public final static String INFERENCE_LOGGER_MODE_REQUEST = "request";
  public final static String INFERENCE_LOGGER_MODE_RESPONSE = "response";
  
  // Labels and annotations
  
  public Map<String, String> getHopsworksServingLabels(Project project, Serving serving) {
    return new HashMap<String, String>() {
      {
        put(SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
        put(SERVING_NAME_LABEL_NAME, serving.getName());
        put(PROJECT_ID_LABEL_NAME, String.valueOf(project.getId()));
        put(CREATOR_LABEL_NAME, serving.getCreator().getUsername());
        put(MODEL_NAME_LABEL_NAME, serving.getModelName());
        put(MODEL_VERSION_LABEL_NAME, serving.getModelVersion().toString());
        put(ARTIFACT_VERSION_LABEL_NAME, serving.getArtifactVersion().toString());
        put(MODEL_SERVER_LABEL_NAME, serving.getModelServer().toString().toLowerCase());
        put(SERVING_TOOL_LABEL_NAME, serving.getServingTool().toString().toLowerCase());
        put(REVISION_LABEL_NAME, serving.getRevision());
      }
    };
  }
  
  public Map<String, String> getHopsworksServingAnnotations(Serving serving) {
    return new HashMap<String, String>() {
      {
        put(ARTIFACT_PATH_ANNOTATION_NAME, kubeArtifactUtils.getArtifactFilePath(serving));
        if (serving.getKafkaTopic() != null) {
          put(TOPIC_NAME_ANNOTATION_NAME, serving.getKafkaTopic().getTopicName());
        }
        if (serving.getPredictor() != null) {
          put(PREDICTOR_ANNOTATION_NAME, serving.getPredictor());
        }
        if (serving.getTransformer() != null) {
          put(TRANSFORMER_ANNOTATION_NAME, serving.getTransformer());
        }
      }
    };
  }

  public Map<String, String> getServingScopeLabels(boolean reserved) {
    return new HashMap<String, String>() {
      {
        put(RESERVED_LABEL_NAME, String.valueOf(reserved));
        put(SCOPE_LABEL_NAME, SCOPE_SERVING_LABEL_VALUE);
      }
    };
  }
  
  // Deployment
  
  public String getNewRevisionID() {
    return RandomStringUtils.randomNumeric(LAST_REVISION_LENGTH);
  }
  
  public Integer getAvailableReplicas(DeploymentStatus deploymentStatus) {
    return deploymentStatus != null && deploymentStatus.getAvailableReplicas() != null
      ? deploymentStatus.getAvailableReplicas()
      : 0;
  }
  
  public String getInternalInferencePath(Serving serving, InferenceVerb verb) {
    if (serving.getServingTool() == ServingTool.KSERVE) {
      return "/v1/models/" + serving.getName() + (verb != null ? verb.toString() : "");
    } else { // default
      KubePredictorServerUtils predictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
      return predictorServerUtils.getDeploymentPath(serving.getName(), serving.getModelVersion(), verb);
    }
  }
  
  // Node selector and tolerations
  
  public Map<String, String> getServingNodeLabels() {
    String nodeLabels = settings.getKubeServingNodeLabels();
    if (nodeLabels.isEmpty()) {
      return null;
    }
    Map<String, String> nodeSelectors = new HashMap<>();
    for (String keyValue : nodeLabels.split(NODE_LABELS_TOLERATIONS_SEPARATOR)) {
      String[] split = Arrays.stream(keyValue.split(NODE_SELECTOR_KEY_VALUE_SEPARATOR))
        .map(String::trim).toArray(String[]::new);
      
      if (split.length != 2) {
        throw new IllegalArgumentException("node label '" + keyValue + "' does not follow the format key=value");
      }
      nodeSelectors.put(split[0], split[1]);
    }
    return nodeSelectors;
  }
  
  public List<Map<String, String>> getServingNodeTolerations() {
    String tolerations = settings.getKubeServingNodeTolerations();
    if (tolerations.isEmpty()) {
      return null;
    }
    List<Map<String, String>> nodeTolerations = new ArrayList<>();
    for (String keyValue : tolerations.split(NODE_LABELS_TOLERATIONS_SEPARATOR)) {
      String[] split = Arrays.stream(keyValue.split(NODE_TOLERATIONS_ATTR_SEPARATOR))
        .map(String::trim).toArray(String[]::new);
      
      if (split.length != 3 && split.length != 4) {
        throw new IllegalArgumentException("node toleration '" + keyValue + "' does not follow the format " +
          "key:operator:[value]:effect");
      }
      
      nodeTolerations.add(new HashMap<String, String>() {{
          put("key", split[0]);
          put("operator", split[1]);
          if (split.length == 4) {
            put("value", split[2]);
          }
          put("effect", split[split.length-1]);
        }}
      );
    }
    return nodeTolerations;
  }
}
