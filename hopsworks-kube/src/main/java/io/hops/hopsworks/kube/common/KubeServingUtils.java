/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;

public final class KubeServingUtils {
  
  private final static Integer LAST_REVISION_LENGTH = 8;
  
  // Labels
  protected final static String LABEL_PREFIX = "serving.hops.works";
  public final static String SERVING_ID_LABEL_NAME = LABEL_PREFIX + "/id";
  public final static String SERVING_NAME_LABEL_NAME = LABEL_PREFIX + "/name";
  public final static String SERVING_TOOL_LABEL_NAME = LABEL_PREFIX + "/tool";
  public final static String PROJECT_ID_LABEL_NAME = LABEL_PREFIX + "/project-id";
  public final static String MODEL_NAME_LABEL_NAME = LABEL_PREFIX + "/model-name";
  public final static String MODEL_VERSION_LABEL_NAME = LABEL_PREFIX + "/model-version";
  public final static String MODEL_SERVER_LABEL_NAME = LABEL_PREFIX + "/model-server";
  public final static String REVISION_LABEL_NAME = LABEL_PREFIX + "/revision";
  public final static String TOPIC_NAME_LABEL_NAME = LABEL_PREFIX + "/topic-name";

  // Inference logger
  public final static String INFERENCE_LOGGER_MODE = "all";
  public final static String INFERENCE_LOGGER_HOST = "localhost";
  public final static Integer INFERENCE_LOGGER_PORT = 9099;

  public static Map<String, String> getHopsworksServingLabels(Project project, Serving serving) {
    HashMap<String, String> labels = new HashMap<String, String>() {
      {
        put(SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
        put(SERVING_NAME_LABEL_NAME, serving.getName());
        put(SERVING_TOOL_LABEL_NAME, serving.getServingTool().toString().toLowerCase());
        put(PROJECT_ID_LABEL_NAME, String.valueOf(project.getId()));
        put(MODEL_NAME_LABEL_NAME, getModelName(serving));
        put(MODEL_VERSION_LABEL_NAME, serving.getVersion().toString());
        put(MODEL_SERVER_LABEL_NAME, serving.getModelServer().toString().toLowerCase());
        put(REVISION_LABEL_NAME, serving.getRevision());
      }
    };
    if (serving.getKafkaTopic() != null) {
      labels.put(TOPIC_NAME_LABEL_NAME, serving.getKafkaTopic().getTopicName());
    }
    
    return labels;
  }
  
  public static String getModelName(Serving serving) {
    String artifactPath = serving.getArtifactPath();
    if (artifactPath.endsWith("/")) {
      artifactPath = artifactPath.substring(0, artifactPath.length() - 1);
    }
    String[] split = artifactPath.split("/");
    return split[split.length - 1];
  }
  
  public static String getNewRevisionID() {
    return RandomStringUtils.randomNumeric(LAST_REVISION_LENGTH);
  }
}
