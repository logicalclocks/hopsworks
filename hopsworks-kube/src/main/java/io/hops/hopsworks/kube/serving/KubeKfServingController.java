/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.common.Pair;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeIstioClientService;
import io.hops.hopsworks.kube.common.KubeKfServingClientService;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTransformerUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
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
public class KubeKfServingController extends KubeToolServingController {

  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeKfServingClientService kubeKfServingClientService;
  @EJB
  private KubeIstioClientService kubeIstioClientService;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  
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
    Pair<String, Integer> externalIngressHostPort;
    Pair<String, Integer> internalIngressHostPort;
    
    try {
      inferenceService = kubeKfServingClientService.getInferenceService(project, serving);
      deploymentStatus = getDeploymentStatus(project, serving, "predictor");
      status = getServingStatus(project, serving, inferenceService);
      if (serving.getTransformer() != null) {
        transformerDeploymentStatus = getDeploymentStatus(project, serving, "transformer");
      }
      internalIngressHostPort = kubeIstioClientService.getIstioIngressHostPort();
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
      JSONObject jsonStatus = inferenceService.getJSONObject("status");
      JSONArray jsonConditions = jsonStatus.has("conditions") ? jsonStatus.getJSONArray("conditions") : new JSONArray();
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
        setAvailable(inferenceService != null);
        setAvailableReplicas(availableReplicas);
        setAvailableTransformerReplicas(finalAvailableTransformerReplicas);
        setConditions(conditions.size() > 0 ? conditions : null);
        setInternalIPs(kubeClientService.getReadyNodeList());
        setInternalPort(internalIngressHostPort.getR());
        setInternalPath(kubeServingUtils.getInternalInferencePath(serving, null));
        // These values will be fetched from the location href in the UI (client-side). By doing this, we make sure
        // that we display the correct host and port to reach Hopsworks. For instance, using proxies or SSH
        // tunneling, the port might differ from the default 80 or 443 on the client side.
        setExternalIP(null);
        setExternalPort(null);
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
  
  private void createOrReplace(Project project, Users user, JSONObject inferenceService) {
    kubeKfServingClientService.createOrReplaceInferenceService(project, inferenceService);
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
    JSONObject status = inferenceService.getJSONObject("status");
    JSONArray conditions = status.has("conditions") ? status.getJSONArray("conditions") : new JSONArray();
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
  
    KubePredictorServerUtils predictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    JSONObject predictor;
    try {
      predictor = predictorServerUtils.buildInferenceServicePredictor(project, user, serving, artifactPath);
    } catch (ServiceDiscoveryException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO, null, e.getMessage(), e);
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
