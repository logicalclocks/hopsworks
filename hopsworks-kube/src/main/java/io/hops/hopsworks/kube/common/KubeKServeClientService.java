/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import org.json.JSONObject;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeKServeClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeKServeClientService.class.getName());
  
  public static final String INFERENCESERVICE_PLURAL = "inferenceservices";
  public static final String INFERENCESERVICE_GROUP = "serving.kserve.io";
  public static final String INFERENCESERVICE_SCOPE = "Namespaced";
  public static final String INFERENCESERVICE_VERSION = "v1beta1";
  public static final String INFERENCESERVICE_KIND = "InferenceService";
  
  @EJB
  private KubeClientService kubeClientService;
  
  @Asynchronous
  public void createOrReplaceInferenceService(Project project, JSONObject inferenceService)
    throws KubernetesClientException {
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
    
    handleClientOp((client) -> client.customResource(context).createOrReplace(kubeProjectNs,
      inferenceService.toString()));
  }
  
  @Asynchronous
  public void deleteInferenceService(Project project, ObjectMeta inferenceServiceMetadata)
    throws KubernetesClientException {
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
    
    handleClientOp((client) -> client.customResource(context).delete(kubeProjectNs,
      inferenceServiceMetadata.getName()));
  }
  
  public JSONObject getInferenceServiceStatus(Project project, Serving serving)
    throws KubernetesClientException {
    JSONObject inferenceService = getInferenceService(project, serving);
    return inferenceService != null && inferenceService.has("status")
      ? inferenceService.getJSONObject(("status"))
      : null;
  }
  
  public JSONObject getInferenceServiceMetadata(Project project, Serving serving)
    throws KubernetesClientException {
    JSONObject inferenceService = getInferenceService(project, serving);
    return inferenceService != null && inferenceService.has("metadata")
      ? inferenceService.getJSONObject(("metadata"))
      : null;
  }
  
  public JSONObject getInferenceService(Project project, Serving serving)
    throws KubernetesClientException {
    CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    Map<String, String> labels = new HashMap<>();
    labels.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    
    Map<String, Object> response = handleClientOp((client) -> client.customResource(context).list(kubeProjectNs,
      labels));
    
    JSONObject inferenceService = null;
    if (response != null) {
      ArrayList<Map<String, Object>> inferenceServices = (ArrayList<Map<String, Object>>) response.get("items");
      if (inferenceServices.size() > 0) {
        inferenceService = new JSONObject(inferenceServices.get(0));
      }
    }
    return inferenceService;
  }
  
  private interface KubeRunner<T> {
    T run(KubernetesClient client) throws KubernetesClientException, IOException;
  }
  
  private <T> T handleClientOp(KubeRunner<T> op) throws KubernetesClientException {
    return kubeClientService.handleClientOp(
      (client) -> {
        try {
          return op.run(client);
        } catch (IOException e) {
          throw new KubernetesClientException(e.getMessage(), e);
        }
      });
  }
  
  private CustomResourceDefinitionContext getCustomResourceDefinitionContext() {
    return new CustomResourceDefinitionContext.Builder()
      .withName(String.format("%s.%s", INFERENCESERVICE_PLURAL, INFERENCESERVICE_GROUP))
      .withGroup(INFERENCESERVICE_GROUP)
      .withScope(INFERENCESERVICE_SCOPE)
      .withVersion(INFERENCESERVICE_VERSION)
      .withPlural(INFERENCESERVICE_PLURAL)
      .build();
  }
}
