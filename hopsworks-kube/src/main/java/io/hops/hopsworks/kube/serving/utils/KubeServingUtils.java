/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeServingUtils {
  
  private final static Integer LAST_REVISION_LENGTH = 8;
  
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubeModelUtils kubeModelUtils;
  
  // Labels
  protected final static String LABEL_PREFIX = "serving.hops.works";
  public final static String SERVING_ID_LABEL_NAME = LABEL_PREFIX + "/id";
  public final static String SERVING_NAME_LABEL_NAME = LABEL_PREFIX + "/name";
  public final static String PROJECT_ID_LABEL_NAME = LABEL_PREFIX + "/project-id";
  public final static String MODEL_NAME_LABEL_NAME = LABEL_PREFIX + "/model-name";
  public final static String MODEL_VERSION_LABEL_NAME = LABEL_PREFIX + "/model-version";
  public final static String ARTIFACT_VERSION_LABEL_NAME = LABEL_PREFIX + "/artifact-version";
  public final static String MODEL_SERVER_LABEL_NAME = LABEL_PREFIX + "/model-server";
  public final static String SERVING_TOOL_LABEL_NAME = LABEL_PREFIX + "/tool";
  public final static String REVISION_LABEL_NAME = LABEL_PREFIX + "/revision";
  
  public final static String COMPONENT_LABEL_NAME = "component";
  
  // Annotations
  public final static String ARTIFACT_PATH_ANNOTATION_NAME = LABEL_PREFIX + "/artifact-path";
  public final static String TRANSFORMER_ANNOTATION_NAME = LABEL_PREFIX + "/transformer";
  public final static String TOPIC_NAME_ANNOTATION_NAME = LABEL_PREFIX + "/topic-name";
  
  // Api key
  private final static String MODEL_SERVING_SECRET_SUFFIX = "--serving";
  private final static String MODEL_SERVING_SECRET_APIKEY_NAME = "apiKey";
  
  // Inference logger
  public final static String INFERENCE_LOGGER_HOST = "localhost";
  public final static Integer INFERENCE_LOGGER_PORT = 9099;
  public final static String INFERENCE_LOGGER_MODE_ALL = "all";
  public final static String INFERENCE_LOGGER_MODE_REQUEST = "request";
  public final static String INFERENCE_LOGGER_MODE_RESPONSE = "response";
  
  public Map<String, String> getHopsworksServingLabels(Project project, Serving serving) {
    return new HashMap<String, String>() {
      {
        put(SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
        put(SERVING_NAME_LABEL_NAME, serving.getName());
        put(PROJECT_ID_LABEL_NAME, String.valueOf(project.getId()));
        put(MODEL_NAME_LABEL_NAME, kubeModelUtils.getModelName(serving));
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
        if (serving.getKafkaTopic()!= null) {
          put(TOPIC_NAME_ANNOTATION_NAME, serving.getKafkaTopic().getTopicName());
        }
        if (serving.getTransformer() != null) {
          put(TRANSFORMER_ANNOTATION_NAME, serving.getTransformer());
        }
      }
    };
  }
  
  public String getApiKeyName(String prefix) {
    return prefix.toLowerCase() + MODEL_SERVING_SECRET_SUFFIX;
  }
  
  public String getApiKeySecretName(String prefix) {
    return prefix + MODEL_SERVING_SECRET_SUFFIX;
  }
  
  public String getApiKeySecretKey() {
    return MODEL_SERVING_SECRET_APIKEY_NAME;
  }
  
  public String getNewRevisionID() {
    return RandomStringUtils.randomNumeric(LAST_REVISION_LENGTH);
  }
  
  public Integer getAvailableReplicas(DeploymentStatus deploymentStatus) {
    return deploymentStatus != null && deploymentStatus.getAvailableReplicas() != null
      ? deploymentStatus.getAvailableReplicas()
      : 0;
  }
}
