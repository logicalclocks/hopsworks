/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingType;

import java.util.HashMap;
import java.util.Map;

final class KubeServingUtils {
  
  protected final static String LABEL_PREFIX = "serving.hops.works";
  protected final static String SERVING_ID_LABEL_NAME = LABEL_PREFIX + "/serving-id";
  protected final static String SERVING_NAME_LABEL_NAME = LABEL_PREFIX + "/serving-name";
  protected final static String PROJECT_ID_LABEL_NAME = LABEL_PREFIX + "/project-id";
  protected final static String MODEL_NAME_LABEL_NAME = LABEL_PREFIX + "/model-name";
  protected final static String MODEL_VERSION_LABEL_NAME = LABEL_PREFIX + "/model-version";
  protected final static String FRAMEWORK_LABEL_NAME = LABEL_PREFIX + "/framework";
  protected final static String TOOL_LABEL_NAME = LABEL_PREFIX + "/tool";
  
  protected static Map<String, String> getHopsworksServingLabels(Integer projectId, String servingId,
    String servingName, String modelName, Integer modelVersion, ServingType servingType, String servingToolName) {
    return new HashMap<String, String>() {
      {
        put(SERVING_ID_LABEL_NAME, servingId);
        put(SERVING_NAME_LABEL_NAME, servingName);
        put(PROJECT_ID_LABEL_NAME, projectId.toString());
        put(MODEL_NAME_LABEL_NAME, modelName);
        put(MODEL_VERSION_LABEL_NAME, modelVersion.toString());
        put(FRAMEWORK_LABEL_NAME, KubeServingUtils.getFramework(servingType));
        put(TOOL_LABEL_NAME, servingToolName);
      }
    };
  }
  
  protected static String getFramework(ServingType servingType) {
    String type = servingType.toString();
    if (type.contains("_")) {
      type = type.split("_")[1];
    }
    return type.toLowerCase();
  }
  
  protected static String getModelName(Serving serving) {
    String artifactPath = serving.getArtifactPath();
    if (artifactPath.endsWith("/")) {
      artifactPath = artifactPath.substring(0, artifactPath.length() - 1);
    }
    String[] split = artifactPath.split("/");
    return split[split.length - 1];
  }
}
