/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;

import java.util.HashMap;
import java.util.Map;

public final class KubeServingUtils {
  
  protected final static String LABEL_PREFIX = "serving.hops.works";
  public final static String SERVING_ID_LABEL_NAME = LABEL_PREFIX + "/id";
  public final static String SERVING_NAME_LABEL_NAME = LABEL_PREFIX + "/name";
  public final static String SERVING_TOOL_LABEL_NAME = LABEL_PREFIX + "/tool";
  public final static String PROJECT_ID_LABEL_NAME = LABEL_PREFIX + "/project-id";
  public final static String MODEL_NAME_LABEL_NAME = LABEL_PREFIX + "/model-name";
  public final static String MODEL_VERSION_LABEL_NAME = LABEL_PREFIX + "/model-version";
  public final static String MODEL_SERVER_LABEL_NAME = LABEL_PREFIX + "/model-server";
  
  public static Map<String, String> getHopsworksServingLabels(Integer projectId, String servingId,
    String servingName, String modelName, Integer modelVersion, ModelServer modelServer, ServingTool servingTool) {
    return new HashMap<String, String>() {
      {
        put(SERVING_ID_LABEL_NAME, servingId);
        put(SERVING_NAME_LABEL_NAME, servingName);
        put(SERVING_TOOL_LABEL_NAME, servingTool.toString().toLowerCase());
        put(PROJECT_ID_LABEL_NAME, projectId.toString());
        put(MODEL_NAME_LABEL_NAME, modelName);
        put(MODEL_VERSION_LABEL_NAME, modelVersion.toString());
        put(MODEL_SERVER_LABEL_NAME, modelServer.toString().toLowerCase());
      }
    };
  }
  
  public static String getModelName(Serving serving) {
    String artifactPath = serving.getArtifactPath();
    if (artifactPath.endsWith("/")) {
      artifactPath = artifactPath.substring(0, artifactPath.length() - 1);
    }
    String[] split = artifactPath.split("/");
    return split[split.length - 1];
  }
}
