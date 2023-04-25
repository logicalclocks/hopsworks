/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
  public void createInferenceService(Project project, JSONObject inferenceService)
      throws KubernetesClientException {
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    try {
      GenericKubernetesResource customResource = Serialization.jsonMapper()
          .readValue(inferenceService.toString(), GenericKubernetesResource.class);
      CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
      handleClientOp((client) -> client.genericKubernetesResources(context)
          .inNamespace(kubeProjectNs).resource(customResource).create());
    } catch (JsonProcessingException e) {
      LOGGER.log(Level.FINE,"Inference service json serialization error", e);
      throw new KubernetesClientException(e.getMessage());
    }
  }
  
  @Asynchronous
  public void updateInferenceService(Project project, JSONObject inferenceService)
    throws KubernetesClientException {
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    try {
      GenericKubernetesResource customResource = Serialization.jsonMapper()
        .readValue(inferenceService.toString(), GenericKubernetesResource.class);
      CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
      handleClientOp((client) -> client.genericKubernetesResources(context)
        .inNamespace(kubeProjectNs).resource(customResource).update());
    } catch (JsonProcessingException e) {
      LOGGER.log(Level.FINE,"Inference service json serialization error", e);
      throw new KubernetesClientException(e.getMessage());
    }
  }
  
  @Asynchronous
  public void deleteInferenceService(Project project, ObjectMeta inferenceServiceMetadata)
    throws KubernetesClientException {
    String kubeProjectNs = kubeClientService.getKubeProjectName(project);
    CustomResourceDefinitionContext context = getCustomResourceDefinitionContext();
    
    handleClientOp((client) -> client.genericKubernetesResources(context)
        .inNamespace(kubeProjectNs).withName(inferenceServiceMetadata.getName()).delete());
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

    GenericKubernetesResourceList response = handleClientOp((client) -> client.genericKubernetesResources(context)
        .inNamespace(kubeProjectNs).withLabels(labels).list());
    
    JSONObject inferenceService = null;
    if (response != null && response.getItems().size() > 0) {
      inferenceService = new JSONObject(Serialization.asJson(response.getItems().get(0)));
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
