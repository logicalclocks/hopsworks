/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeIstioClientService;
import io.hops.hopsworks.kube.common.KubeKfServingClientService;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTfServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTransformerUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.NotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeKfServingController extends KubeToolServingController {
  
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeKfServingClientService kubeKfServingClientService;
  @EJB
  private KubeIstioClientService kubeIstioClientService;
  @EJB
  private ApiKeyController apiKeyController;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeTfServingUtils kubeTfServingUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    createOrReplace(project, user, buildInferenceService(project, user, serving));
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      JSONObject metadata = kubeKfServingClientService.getInferenceServiceMetadata(project, serving);
      if (metadata != null) {
        // When updating an inference service, the current resource version must be indicated
        String resourceVersion = metadata.getString("resourceVersion");
        createOrReplace(project, user, buildInferenceService(project, user, serving, resourceVersion));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    try {
      JSONObject metadata = kubeKfServingClientService.getInferenceServiceMetadata(project, serving);
      if (metadata != null) {
        kubeKfServingClientService
          .deleteInferenceService(project, getInferenceServiceMetadataObject(project, serving));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    ServingStatusEnum status;
    JSONObject inferenceService;
    DeploymentStatus deploymentStatus;
    DeploymentStatus transformerDeploymentStatus = null;
    Pair<String, Integer> ingressHostPort;
    
    try {
      inferenceService = kubeKfServingClientService.getInferenceService(project, serving);
      deploymentStatus = getDeploymentStatus(project, serving, "predictor");
      status = getServingStatus(project, serving, inferenceService);
      if (serving.getTransformer() != null) {
        transformerDeploymentStatus = getDeploymentStatus(project, serving, "transformer");
      }
      ingressHostPort = kubeIstioClientService.getIstioIngressHostPort();
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  
    Integer availableReplicas = kubeServingUtils.getAvailableReplicas(deploymentStatus);
    Integer availableTransformerReplicas = null;
    if (serving.getTransformer() != null) {
      availableTransformerReplicas = kubeServingUtils.getAvailableReplicas(transformerDeploymentStatus);
    }

    // These variables are accessed from within inner class, needs to be final or effectively final
    ServingStatusEnum finalStatus = status;
    Integer finalAvailableTransformerReplicas = availableTransformerReplicas;
    
    List<String> conditions = new ArrayList<>();
    if (inferenceService != null && inferenceService.has("status")) {
      JSONArray jsonConditions = inferenceService.getJSONObject("status").getJSONArray("conditions");
      for (int i = 0; i < jsonConditions.length(); i++) {
        JSONObject condition = jsonConditions.getJSONObject(i);
        if (condition.has("reason") && condition.getString("reason").equals("RevisionFailed")
          && !condition.getString("status").equals("True")) {
          String msg = condition.getString("message");
          String component = condition.getString("type").contains("Predictor") ? "Predictor" : "Transformer";
          conditions.add(component + ":" + (msg.contains(":") ? msg.substring(msg.indexOf(":") + 1) : msg));
        }
      }
    }
    
    return new KubeServingInternalStatus() {
      {
        setServingStatus(finalStatus);
        setNodePort(ingressHostPort.getR());
        setAvailable(inferenceService != null);
        setAvailableReplicas(availableReplicas);
        setAvailableTransformerReplicas(finalAvailableTransformerReplicas);
        setConditions(conditions.size() > 0 ? conditions : null);
      }
    };
  }
  
  public DeploymentStatus getDeploymentStatus(Project project, Serving serving, String component) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    labelMap.put(KubeServingUtils.REVISION_LABEL_NAME, String.valueOf(serving.getRevision()));
    labelMap.put(KubeServingUtils.COMPONENT_LABEL_NAME, component);
    return kubeClientService.getDeploymentStatus(project, labelMap);
  }
  
  private void createOrReplace(Project project, Users user, JSONObject inferenceService) throws ServingException {
    try {
      ensureApiKeySecret(project, user);
      kubeKfServingClientService.createOrReplaceInferenceService(project, inferenceService);
    } catch (ApiKeyException | UserException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO, null, e.getMessage(), e);
    }
  }
  
  private ServingStatusEnum getServingStatus(Project project, Serving serving, JSONObject inferenceService) {
    
    if (serving.getDeployed() == null) {
      // If the serving has not been deployed
      if (inferenceService == null) {
        // and the inference service is not created, check running pods
        List<Pod> podList = getPodList(project, serving, null);
        return podList.isEmpty()
          ? ServingStatusEnum.STOPPED
          : ServingStatusEnum.STOPPING;
      }
      // Otherwise, the serving is still stopping
      return ServingStatusEnum.STOPPING;
    }
    
    // If the serving has been deployed
    if (inferenceService == null || !inferenceService.has("metadata") || !inferenceService.has("status")) {
      // but the inference service is not created, the serving is still starting
      return ServingStatusEnum.STARTING;
    }

    // Otherwise, check inference service conditions
    JSONArray conditions = inferenceService.getJSONObject("status").getJSONArray("conditions");
    for (int i = 0; i < conditions.length(); i++) {
      JSONObject condition = conditions.getJSONObject(i);
      if (condition.getString("type").endsWith("Ready") && !condition.getString("status").equals("True")) {
        JSONObject metadata = inferenceService.getJSONObject("metadata");
        String revision = metadata.getJSONObject("labels").getString(KubeServingUtils.REVISION_LABEL_NAME);
        return metadata.getInt("generation") == 1 && revision.equals(serving.getRevision())
          ? ServingStatusEnum.STARTING
          : ServingStatusEnum.UPDATING;
      }
    }
    
    // If nº of available replicas matches or scale-to-zero enabled, the serving is running
    return ServingStatusEnum.RUNNING;
  }
  
  private void ensureApiKeySecret(Project project, Users user) throws ApiKeyException, UserException {
    // One apikey per project is created for model serving. This apikey is stored in a kubernetes secret in
    // the namespace of the project and, therefore, removed together with the namespace when a project is deleted.
    String apiKeyName = kubeServingUtils.getApiKeyName(project.getName());
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
      } else {
        // If apiKey exists but the secret doesn't, create a new apiKey and secret.
        apiKeyController.deleteKey(user, apiKeyName);
      }
    }
  
    // If apikey or secret doesn't exist, create a new apikey and secret.
    Set<ApiScope> scopes = new HashSet<ApiScope>() {
      {
        add(ApiScope.DATASET_VIEW); // storage-initializer: download the model artifact
        add(ApiScope.KAFKA); // inference-logger: get the topic schema
        add(ApiScope.PROJECT); // transformer: get project details
        add(ApiScope.FEATURESTORE); // transformer: get feature vector and transformations from the feature store
      }
    };
    String key = apiKeyController.createNewKey(user, apiKeyName, scopes);
    
    // Create or update secret
    String kubeProjectNS = kubeClientService.getKubeProjectName(project);
    String secretName = kubeServingUtils.getApiKeySecretName(kubeProjectNS);
    kubeClientService.createOrUpdateSecret(kubeProjectNS, secretName, new HashMap<String, byte[]>() {
      {
        put(kubeServingUtils.getApiKeySecretKey(), key.getBytes());
      }
    }, labels);
  }
  
  private List<Pod> getPodList(Project project, Serving serving, String component) {
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put(KubeServingUtils.SERVING_ID_LABEL_NAME, String.valueOf(serving.getId()));
    labelMap.put(KubeServingUtils.REVISION_LABEL_NAME, String.valueOf(serving.getRevision()));
    if (component != null) {
      labelMap.put(KubeServingUtils.COMPONENT_LABEL_NAME, component);
    }
    return kubeClientService.getPodList(project, labelMap);
  }

  private JSONObject buildInferenceService(Project project, Users user, Serving serving) throws ServingException {
    return buildInferenceService(project, user, serving, null);
  }
  private JSONObject buildInferenceService(Project project, Users user, Serving serving, String resourceVersion)
      throws ServingException {
    
    String artifactPath = kubeArtifactUtils.getArtifactFilePath(serving);
    JSONObject pipeline = new JSONObject();
    
    JSONObject predictor;
    switch (serving.getModelServer()) {
      case TENSORFLOW_SERVING:
        predictor = kubeTfServingUtils.buildInferenceServicePredictor(artifactPath, serving.getInstances(),
          serving.getInferenceLogging(), serving.getDockerResourcesConfig());
        break;
      default:
        throw new NotSupportedException("Model server not supported for KFServing inference services");
    }
  
    // Add node selectors if defined
    JSONObject nodeSelector = null;
    Map<String, String> nodeSelectorLabels = kubeServingUtils.getServingNodeLabels();
    if (nodeSelectorLabels != null) {
      nodeSelector = new JSONObject(nodeSelectorLabels);
      predictor.put("nodeSelector", nodeSelector);
    }
    
    // Add node tolerations if defined
    JSONArray tolerations = null;
    List<Map<String, String>> nodeTolerations = kubeServingUtils.getServingNodeTolerations();
    if (nodeTolerations != null) {
      tolerations = new JSONArray(nodeTolerations.stream().map(JSONObject::new).collect(Collectors.toList()));
      predictor.put("tolerations", tolerations);
    }
    
    pipeline.put("predictor", predictor);
    
    JSONObject metadata = getInferenceServingMetadataJSON(project, serving);
    if (resourceVersion != null) {
      metadata.put("resourceVersion", resourceVersion);
    }
    
    // Add transformer if defined
    if (serving.getTransformer() != null) {
      try {
        JSONObject transformer = kubeTransformerUtils.buildInferenceServiceTransformer(project, user, serving);
        if (nodeSelector != null) {
          transformer.put("nodeSelector", nodeSelector);
        }
        if (tolerations != null) {
          transformer.put("tolerations", tolerations);
        }
        pipeline.put("transformer", transformer);
      } catch (ServiceDiscoveryException e) {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO, null, e.getMessage(), e);
      }
    }
    
    JSONObject inferenceService = new JSONObject() {
      {
        put("apiVersion", String.format("%s/%s", KubeKfServingClientService.INFERENCESERVICE_GROUP,
          KubeKfServingClientService.INFERENCESERVICE_VERSION));
        put("kind", KubeKfServingClientService.INFERENCESERVICE_KIND);
        put("metadata", metadata);
        put("spec", pipeline);
      }
    };
    
    return inferenceService;
  }
  
  private ObjectMeta getInferenceServiceMetadataObject(Project project, Serving serving) {
    return new ObjectMetaBuilder()
      .withName(serving.getName())
      .withLabels(kubeServingUtils.getHopsworksServingLabels(project, serving))
      .withAnnotations(kubeServingUtils.getHopsworksServingAnnotations(serving))
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
        put("annotations", new JSONObject() {
          {
            for (Map.Entry<String, String> annotation : metadata.getAnnotations().entrySet()) {
              put(annotation.getKey(), annotation.getValue());
            }
          }
        });
      }
    };
  }
}
