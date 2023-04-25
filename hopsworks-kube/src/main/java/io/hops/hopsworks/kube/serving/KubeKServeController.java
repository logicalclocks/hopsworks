/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.serving.ServingLogs;
import io.hops.hopsworks.common.serving.ServingStatusCondition;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeKServeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubeJsonUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTransformerUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeKServeController extends KubeToolServingController {

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeKServeClientService kubeKServeClientService;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @EJB
  private KubeJsonUtils kubeJsonUtils;
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    kubeKServeClientService.createInferenceService(project, buildInferenceService(project, user, serving));
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    kubeKServeClientService.updateInferenceService(project, buildInferenceService(project, user, serving));
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    try {
      JSONObject metadata = kubeKServeClientService.getInferenceServiceMetadata(project, serving);
      if (metadata != null) {
        kubeKServeClientService
          .deleteInferenceService(project, getInferenceServiceMetadataObject(project, serving));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETION_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    Pair<ServingStatusEnum, ServingStatusCondition> statusAndCondition;
    JSONObject inferenceService;
    List<Pod> pods;
    try {
      inferenceService = kubeKServeClientService.getInferenceService(project, serving);
      pods = getPods(project, serving, null);
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUS_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  
    // Get deployment status and condition
    statusAndCondition = getServingStatusAndCondition(serving, inferenceService, pods);
    
    // Get number of running replicas
    Map<String, Long> replicas = pods.stream().parallel().collect(
      Collectors.groupingBy(p -> p.getMetadata().getLabels().getOrDefault("component", "predictor"),
        Collectors.counting()));
    Integer availablePredReplicas = replicas.getOrDefault("predictor", 0L).intValue();
    Integer availableTransReplicas = replicas.getOrDefault("transformer", 0L).intValue();
    
    return new KubeServingInternalStatus() {
      {
        setServingStatus(statusAndCondition.getValue0());
        setAvailable(inferenceService != null);
        setAvailableReplicas(availablePredReplicas);
        setAvailableTransformerReplicas(availableTransReplicas);
        setCondition(statusAndCondition.getValue1());
        setModelServerInferencePath(kubeServingUtils.getModelServerInferencePath(serving, null));
        setHopsworksInferencePath(kubeServingUtils.getHopsworksInferencePath(serving, null));
      }
    };
  }
  
  @Override
  public List<ServingLogs> getLogs(Project project, Serving serving, String component, Integer tailingLines) {
    ArrayList<ServingLogs> logs = new ArrayList<>();
    List<Pod> pods = getPods(project, serving, component);
    for (Pod pod : pods) {
      String content = kubeClientService.getLogs(pod.getMetadata().getNamespace(), pod.getMetadata().getName(),
        KubeServingUtils.KSERVE_CONTAINER, tailingLines, KubeServingUtils.LIMIT_BYTES);
      logs.add(new ServingLogs(pod.getMetadata().getName(), content));
    }
    return logs;
  }
  
  private Pair<ServingStatusEnum, ServingStatusCondition> getServingStatusAndCondition(Serving serving,
    JSONObject inferenceService, List<Pod> pods) {
    
    // Detect created, stopped or stopping deployments.
    if (serving.getDeployed() == null) {
      // if the deployment is not deployed, it can be stopping, stopped or just created.
      Boolean artifactFileExists = kubeArtifactUtils.checkArtifactFileExists(serving);
      ServingStatusCondition condition = kubeServingUtils.getDeploymentCondition(serving.getDeployed(),
        artifactFileExists, pods);
      if (inferenceService == null) {
        // if the inference service is not created, check running pods
        if (pods.isEmpty()) {
          // if no pods are running, inference service is stopped, created or creatin
          ServingStatusEnum status = artifactFileExists
            ? (serving.getRevision() == null ? ServingStatusEnum.CREATED : ServingStatusEnum.STOPPED)
            : ServingStatusEnum.CREATING;
          return new Pair<>(status, condition);
        }
      }
      // Otherwise, the serving is still stopping
      return new Pair<>(ServingStatusEnum.STOPPING, condition);
    }
    
    // If the deployment has been deployed
    if (inferenceService == null || !inferenceService.has("metadata") || !inferenceService.has("status")
      || !inferenceService.getJSONObject("status").has("conditions")) {
      // but the inference service is not created, the deployment is still starting
      return new Pair<>(
        ServingStatusEnum.STARTING,
        ServingStatusCondition.getScheduledInProgressCondition()
      );
    }

    // Otherwise, check inference service conditions
    JSONObject status = inferenceService.getJSONObject("status");
    if (!status.has("conditions")) {
      // if conditions are not available yet, the serving is still starting
      return new Pair<>(
        ServingStatusEnum.STARTING,
        ServingStatusCondition.getScheduledInProgressCondition()
      );
    }
    JSONArray conditions = status.getJSONArray("conditions");
    
    // Extract ready condition
    JSONObject ready = null;
    for (int i = 0; i < conditions.length(); i++) {
      JSONObject condition = conditions.getJSONObject(i);
      if (condition.getString("type").equals("Ready")) {
        ready = condition;
      }
    }
    
    // Check inference service ready status
    Boolean artifactFileExists = kubeArtifactUtils.checkArtifactFileExists(serving);
    String readyStatus = ready != null ? ready.getString("status") : "False";
    if (readyStatus.equals("False") || readyStatus.equals("Unknown")) {
      // Sometimes Knative fails to reconcile the ingress with a warning message: "object has been updated". In this
      // case, KServe temporarily sets Ready to False, until Knative reconciles the ingress successfully.
      ServingStatusCondition condition = kubeServingUtils.getDeploymentCondition(serving.getDeployed(),
        artifactFileExists, pods);
      if (condition.getStatus() != null && !condition.getStatus()) {
        // if the status is False, it failed
        return artifactFileExists
          ? new Pair<>(ServingStatusEnum.FAILED, condition)
          : new Pair<>(ServingStatusEnum.CREATING, condition);
      }

      // otherwise, it is starting or updating
      JSONObject metadata = inferenceService.getJSONObject("metadata");
      String revision = metadata.getJSONObject("labels").getString(KubeServingUtils.REVISION_LABEL_NAME);
      ServingStatusEnum servingStatus = metadata.getInt("generation") == 1 && revision.equals(serving.getRevision())
        ? ServingStatusEnum.STARTING
        : ServingStatusEnum.UPDATING;
      return new Pair<>(servingStatus, condition);
    }
  
    // if the status is True, the inference service is either running or idle.
    if (pods.isEmpty()) {
      // if no pods running, the deployment scaled to zero replicas
      return artifactFileExists
        ? new Pair<>(
        ServingStatusEnum.IDLE,
        ServingStatusCondition.getReadySuccessCondition(kubeServingUtils.READY_SUCCESS_IDLE_CONDITION_MESSAGE)
      )
        : new Pair<>(
        ServingStatusEnum.CREATING,
        ServingStatusCondition.getStoppedCreatingCondition()
      );
    }
    // if no available predictors or no available transformers, deployment is IDLE
    Integer predReplicas = 0, transReplicas = 0;
    for (Pod pod : pods) {
      String component = pod.getMetadata().getLabels().getOrDefault("component", "predictor");
      if (component.equals("predictor")) {
        predReplicas += 1;
      } else {
        transReplicas += 1;
      }
    }
    if (predReplicas == 0 || (serving.getTransformer() != null && transReplicas == 0)) {
      return artifactFileExists
        ? new Pair<>(
        ServingStatusEnum.IDLE,
        ServingStatusCondition.getReadySuccessCondition(kubeServingUtils.READY_SUCCESS_IDLE_CONDITION_MESSAGE)
      )
        : new Pair<>(
        ServingStatusEnum.CREATING,
        ServingStatusCondition.getStoppedCreatingCondition()
      );
    }
    return new Pair<>(
      ServingStatusEnum.RUNNING,
      ServingStatusCondition.getReadySuccessCondition()
    );
  }
  
  private List<Pod> getPods(Project project, Serving serving, String component) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    labelMap.put(KubeServingUtils.REVISION_LABEL_NAME, String.valueOf(serving.getRevision()));
    if (component != null) {
      labelMap.put(KubeServingUtils.COMPONENT_LABEL_NAME, component);
    }
    return kubeClientService.getPods(project, labelMap);
  }

  private JSONObject buildInferenceService(Project project, Users user, Serving serving) throws ServingException {
    return buildInferenceService(project, user, serving, null);
  }

  private JSONObject buildInferenceService(Project project, Users user, Serving serving, String resourceVersion)
    throws ServingException {
    
    // Metadata
    JSONObject metadata = kubeJsonUtils.buildInferenceServiceMetadata(getInferenceServiceMetadataObject(project,
      serving));
    
    // Predictor
    KubePredictorServerUtils predictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    JSONObject predictor;
    try {
      String artifactPath = kubeArtifactUtils.getArtifactFilePath(serving);
      predictor = predictorServerUtils.buildInferenceServicePredictor(project, user, serving, artifactPath);
    } catch (ServiceDiscoveryException | ApiKeyException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR_INT, Level.INFO, null, e.getMessage(), e);
    }

    // Add transformer if defined
    JSONObject transformer = null;
    if (serving.getTransformer() != null) {
      try {
        transformer = kubeTransformerUtils.buildInferenceServiceTransformer(project, user, serving);
      } catch (ServiceDiscoveryException | ApiKeyException e) {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR_INT, Level.INFO, null, e.getMessage(), e);
      }
    }
    
    return kubeJsonUtils.buildInferenceService(predictor, metadata, resourceVersion, transformer);
  }
  
  private ObjectMeta getInferenceServiceMetadataObject(Project project, Serving serving) {
    return new ObjectMetaBuilder()
      .withName(serving.getName())
      .withLabels(kubeServingUtils.getHopsworksServingLabels(project, serving))
      .withAnnotations(kubeServingUtils.getHopsworksServingAnnotations(serving))
      .build();
  }
}
