/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.json.JSONObject;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeKfServingClientService {
  
  private static final Logger LOGGER = Logger.getLogger(KubeKfServingClientService.class.getName());
  
  public static final String INFERENCESERVICE_PLURAL = "inferenceservices";
  public static final String INFERENCESERVICE_GROUP = "serving.kubeflow.org";
  public static final String INFERENCESERVICE_SCOPE = "Namespaced";
  public static final String INFERENCESERVICE_VERSION = "v1alpha2";
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
