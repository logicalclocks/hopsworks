/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyController;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeIstioClientService;
import io.hops.hopsworks.kube.common.KubeKfServingClientService;
import io.hops.hopsworks.kube.common.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.NotSupportedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeKfServingController extends KubeToolServingController {
  
  private final static String MODEL_SERVING_SECRET_SUFFIX = "--serving";
  private final static String MODEL_SERVING_SECRET_APIKEY_NAME = "apiKey";
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeKfServingClientService kubeKfServingClientService;
  @EJB
  private KubeIstioClientService kubeIstioClientService;
  @EJB
  private KubeTfServingUtils kubeTfServingUtils;
  @EJB
  private ApiKeyController apiKeyController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      zipArtifact(project, user, serving);
      ensureApiKeySecret(project, user);
      kubeKfServingClientService.createOrReplaceInferenceService(project, buildInferenceService(project, serving));
    } catch (ApiKeyException | UserException | DatasetException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      DeploymentStatus deploymentStatus = getDeploymentStatus(project, serving);
      if (deploymentStatus != null) {
        createInstance(project, user, serving);
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    try {
      DeploymentStatus deploymentStatus = getDeploymentStatus(project, serving);
      if (deploymentStatus != null) {
        kubeKfServingClientService
          .deleteInferenceService(project, getInferenceServiceMetadataObject(project, serving));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    DeploymentStatus deploymentStatus;
    List<Pod> podList;
    Pair<String, Integer> ingressHostPort;
    try {
      deploymentStatus = getDeploymentStatus(project, serving);
      podList = getPodList(project, serving);
      ingressHostPort = kubeIstioClientService.getIstioIngressHostPort();
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE, null, e.getMessage(), e);
    }
    
    ServingStatusEnum status = getServingStatus(serving, deploymentStatus, podList);
    Integer availableReplicas = deploymentStatus == null ? null : deploymentStatus.getAvailableReplicas();
    
    return new KubeServingInternalStatus() {
      {
        setServingStatus(status);
        setNodePort(ingressHostPort.getR());
        setAvailableReplicas(availableReplicas);
      }
    };
  }
  
  public DeploymentStatus getDeploymentStatus(Project project, Serving serving) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    return kubeClientService.getDeploymentStatus(project, labelMap);
  }
  
  private ServingStatusEnum getServingStatus(Serving serving, DeploymentStatus deploymentStatus, List<Pod> podList) {
    if (deploymentStatus != null) {
      Integer availableReplicas = deploymentStatus.getAvailableReplicas();
      if (availableReplicas == null ||
        (!availableReplicas.equals(serving.getInstances()) && deploymentStatus.getObservedGeneration() == 1)) {
        // if there is a mismatch between the requested number of instances and the number actually active
        // in Kubernetes, and it's the 1st generation, the serving cluster is starting
        return ServingStatusEnum.STARTING;
      } else if (availableReplicas.equals(serving.getInstances())) {
        return ServingStatusEnum.RUNNING;
      } else {
        return ServingStatusEnum.UPDATING;
      }
    } else {
      if (podList.isEmpty()) {
        return ServingStatusEnum.STOPPED;
      } else {
        // If there are still Pod running, we are still in the stopping phase.
        return ServingStatusEnum.STOPPING;
      }
    }
  }
  
  private void ensureApiKeySecret(Project project, Users user) throws ApiKeyException, UserException {
    // One apikey per project is created for model serving. This apikey is stored in a kubernetes secret in
    // the namespace of the project and, therefore, removed together with the namespace when a project is deleted.
    String apiKeyName = project.getName().toLowerCase() + MODEL_SERVING_SECRET_SUFFIX;
    List<ApiKey> apiKeys = apiKeyController.getKeys(user);
    Optional<ApiKey> apiKey =
      apiKeys.stream().filter(key -> key.getName().equals(apiKeyName)).findFirst();
  
    // ApiKey secret labels
    Map<String, String> labels = new HashMap<>(1);
    labels.put("project-name", project.getName());
    labels.put("scope", "serving");
    
    if (apiKey.isPresent()) {
      List<Secret> secrets = kubeClientService.getSecrets(labels);
      if (secrets != null && !secrets.isEmpty()) {
        // If apiKey and secret exists, return
        return;
      }
      else {
        // If apiKey exists but the secret doesn't, create a new apiKey and secret.
        apiKeyController.deleteKey(user, apiKeyName);
      }
    }
  
    // If apikey or secret doesn't exist, create a new apikey and secret.
    Set<ApiScope> scopes = new HashSet<ApiScope>() {
      {
        add(ApiScope.DATASET_VIEW);
      }
    };
    String key = apiKeyController.createNewKey(user, apiKeyName, scopes);
    
    // Create or update secret
    String kubeProjectNS = kubeClientService.getKubeProjectName(project);
    String secretName = kubeProjectNS + MODEL_SERVING_SECRET_SUFFIX;
    kubeClientService.createOrUpdateSecret(kubeProjectNS, secretName, new HashMap<String, byte[]>() {
      {
        put(MODEL_SERVING_SECRET_APIKEY_NAME, key.getBytes());
      }
    }, labels);
  }
  
  private void zipArtifact(Project project, Users user, Serving serving)
    throws DatasetException {
    String versionedArtifactDir = getVersionedArtifactPath(serving);
    Path artifactPath = datasetHelper.getDatasetPath(project, versionedArtifactDir, DatasetType.DATASET).getFullPath();
    // If the artifact already exists, it does nothing.
    datasetController.zip(project, user, artifactPath, artifactPath);
  }
  
  private List<Pod> getPodList(Project project, Serving serving) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    return kubeClientService.getPodList(project, labelMap);
  }
  
  private String getVersionedArtifactPath(Serving serving) {
    String versionedArtifactDir = serving.getArtifactPath();
    if (!versionedArtifactDir.endsWith("/")) {
      versionedArtifactDir += "/";
    }
    return versionedArtifactDir + serving.getVersion();
  }
  
  private JSONObject buildInferenceService(Project project, Serving serving) {
    
    String versionedArtifactPath = getVersionedArtifactPath(serving);
    
    JSONObject predictor;
    switch (serving.getModelServer()) {
      case TENSORFLOW_SERVING:
        predictor = kubeTfServingUtils.buildInferenceServicePredictor(versionedArtifactPath, serving.getInstances());
        break;
      default:
        throw new NotSupportedException("Model server not supported for KFServing inference services");
    }
    
    return new JSONObject() {
      {
        put("apiVersion", String.format("%s/%s", KubeKfServingClientService.INFERENCESERVICE_GROUP,
          KubeKfServingClientService.INFERENCESERVICE_VERSION));
        put("kind", KubeKfServingClientService.INFERENCESERVICE_KIND);
        put("metadata", getInferenceServingMetadataJSON(project, serving));
        put("spec", new JSONObject() {
          {
            put("default", predictor);
          }
        });
      }
    };
  }
  
  private ObjectMeta getInferenceServiceMetadataObject(Project project, Serving serving) {
    String servingId = String.valueOf(serving.getId());
    
    return new ObjectMetaBuilder()
      .withName(serving.getName())
      .withLabels(KubeServingUtils.getHopsworksServingLabels(project.getId(), servingId, serving.getName(),
        KubeServingUtils.getModelName(serving), serving.getVersion(), serving.getModelServer(),
          serving.getServingTool()))
      .build();
  }
  
  private JSONObject getInferenceServingMetadataJSON(Project project, Serving serving) {
    ObjectMeta metadata = getInferenceServiceMetadataObject(project, serving);
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
      }
    };
  }
}
