/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
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
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingType;
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
  
  private final static String MODEL_SERVING_APIKEY_NAME = "model-serving";
  private final static String MODEL_SERVING_SECRET_SUFFIX = "--serving";
  private final static String MODEL_SERVING_SECRET_APIKEY_NAME = "apiKey";
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeKfServingClientService kubeKfServingClientService;
  @EJB
  private KubeIstioClientService kubeIstioClientService;
  @EJB
  private KubeTfServingController kubeTfServingController;
  @EJB
  private ApiKeyController apiKeyController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  
  public KubeKfServingController() {
    super("kfserving");
  }
  
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
    Integer nodePort;
    try {
      deploymentStatus = getDeploymentStatus(project, serving);
      podList = getPodList(project, serving);
      nodePort = kubeIstioClientService.getIstioIngressNodePort();
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE, null, e.getMessage(), e);
    }
    
    ServingStatusEnum status = getServingStatus(serving, deploymentStatus, podList);
    Integer availableReplicas = deploymentStatus == null ? null : deploymentStatus.getAvailableReplicas();
    
    return new KubeServingInternalStatus() {
      {
        setServingStatus(status);
        setNodePort(nodePort);
        setAvailableReplicas(availableReplicas);
      }
    };
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
    List<ApiKey> apiKeys = apiKeyController.getKeys(user);
    Optional<ApiKey> apiKey =
      apiKeys.stream().findFirst().filter(key -> key.getName().equals(MODEL_SERVING_APIKEY_NAME));
    
    // Return, in case it already exists
    if (apiKey.isPresent()) {
      return;
    }
    
    // Create apikey
    Set<ApiScope> scopes = new HashSet<ApiScope>() {
      {
        add(ApiScope.DATASET_VIEW);
      }
    };
    String key = apiKeyController.createNewKey(user, MODEL_SERVING_APIKEY_NAME, scopes);
    
    // Create secret
    String kubeProjectNS = kubeClientService.getKubeProjectName(project);
    String secretName = kubeProjectNS + MODEL_SERVING_SECRET_SUFFIX;
    kubeClientService.createOrUpdateSecret(kubeProjectNS, secretName, new HashMap<String, byte[]>() {
      {
        put(MODEL_SERVING_SECRET_APIKEY_NAME, key.getBytes());
      }
    }, null);
  }
  
  private void zipArtifact(Project project, Users user, Serving serving)
    throws DatasetException {
    String versionedArtifactDir = getVersionedArtifactPath(serving);
    Path artifactPath = datasetHelper.getDatasetPath(project, versionedArtifactDir, DatasetType.DATASET).getFullPath();
    // If the artifact already exists, it does nothing.
    datasetController.zip(project, user, artifactPath, artifactPath);
  }
  
  private DeploymentStatus getDeploymentStatus(Project project, Serving serving) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    return kubeClientService.getDeploymentStatus(project, labelMap);
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
    switch (serving.getServingType()) {
      case KFSERVING_TENSORFLOW:
        predictor = kubeTfServingController.buildInferenceServicePredictor(versionedArtifactPath);
        break;
      case TENSORFLOW:
      case SKLEARN:
      default:
        throw new NotSupportedException("Serving type not supported for KFServing inference services");
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
      .withName(getInferenceServiceName(servingId, serving.getServingType()))
      .withLabels(KubeServingUtils.getHopsworksServingLabels(project.getId(), servingId, serving.getName(),
        KubeServingUtils.getModelName(serving), serving.getVersion(), serving.getServingType(), SERVING_TOOL_NAME))
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
  
  private String getInferenceServiceName(String servingId, ServingType servingType) {
    switch (servingType) {
      case KFSERVING_TENSORFLOW:
        return kubeTfServingController.getInferenceServiceName(servingId);
      default:
        throw new NotSupportedException("Serving type not supported for KFServing inference services");
    }
  }
}
