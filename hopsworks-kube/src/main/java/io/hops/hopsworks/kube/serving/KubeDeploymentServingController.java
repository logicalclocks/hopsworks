/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.serving.ServingLogs;
import io.hops.hopsworks.common.serving.ServingStatusCondition;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeDeploymentServingController extends KubeToolServingController {
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      KubePredictorServerUtils kubePredictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
      kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving,
        kubePredictorServerUtils));
      kubeClientService.createOrReplaceService(project, buildService(project, serving, kubePredictorServerUtils));
    } catch (ServiceDiscoveryException | ApiKeyException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR_INT, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    KubePredictorServerUtils kubePredictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    try {
      DeploymentStatus deploymentStatus =
        kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId, kubePredictorServerUtils));
      if (deploymentStatus != null) {
        kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving,
          kubePredictorServerUtils));
      }
    } catch (KubernetesClientException | ServiceDiscoveryException | ApiKeyException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATE_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    KubePredictorServerUtils kubePredictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
        getDeploymentName(servingId, kubePredictorServerUtils));
      
      // If pods are currently running for this serving instance, kill them
      if (deploymentStatus != null) {
        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingId, kubePredictorServerUtils));
      }
      
      Service serviceInfo = kubeClientService.getServiceInfo(project, getServiceName(servingId,
        kubePredictorServerUtils));
      
      // if there is a service, delete it
      if (serviceInfo != null) {
        kubeClientService.deleteService(project, getServiceMetadata(servingId, kubePredictorServerUtils));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETION_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    KubePredictorServerUtils kubePredictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    
    DeploymentStatus deploymentStatus;
    List<Pod> pods;
    Service instanceService;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId,
        kubePredictorServerUtils));
      pods = getPods(project, serving);
      instanceService = kubeClientService.getServiceInfo(project, getServiceName(servingId, kubePredictorServerUtils));
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUS_ERROR, Level.SEVERE,
        "Error while getting service status", e.getMessage(), e);
    }

    // Get deployment status and condition
    Pair<ServingStatusEnum, ServingStatusCondition> statusAndCondition = getServingStatusAndCondition(serving,
      deploymentStatus, pods);
  
    // Get number of running replicas
    Integer availableReplicas = kubeServingUtils.getAvailableReplicas(deploymentStatus);

    return new KubeServingInternalStatus() {
      {
        setServingStatus(statusAndCondition.getValue0());
        setAvailable(deploymentStatus != null && instanceService != null);
        setAvailableReplicas(availableReplicas);
        setCondition(statusAndCondition.getValue1());
        setModelServerInferencePath(kubeServingUtils.getModelServerInferencePath(serving, null));
        setHopsworksInferencePath(kubeServingUtils.getHopsworksInferencePath(serving, null));
      }
    };
  }
  
  @Override
  public List<ServingLogs> getLogs(Project project, Serving serving, String component, Integer tailingLines) {
    ArrayList<ServingLogs> logs = new ArrayList<>();
    List<Pod> pods = getPods(project, serving);
    for (Pod pod : pods) {
      String content = kubeClientService.getLogs(pod.getMetadata().getNamespace(), pod.getMetadata().getName(),
        tailingLines, KubeServingUtils.LIMIT_BYTES);
      logs.add(new ServingLogs(pod.getMetadata().getName(), content));
    }
    return logs;
  }
  
  private Pair<ServingStatusEnum, ServingStatusCondition> getServingStatusAndCondition(Serving serving,
    DeploymentStatus deploymentStatus, List<Pod> pods) {
    
    // Detect created, stopped or stopping deployments.
    if (serving.getDeployed() == null) {
      // if the deployment is not deployed, it can be stopping, stopped or just created
      Boolean artifactFileExists = kubeArtifactUtils.checkArtifactFileExists(serving);
      ServingStatusCondition condition = kubeServingUtils.getDeploymentCondition(serving.getDeployed(),
        artifactFileExists, pods);
      if (deploymentStatus == null) {
        // and the deployment spec is not created, check running pods
        if (pods.isEmpty()) {
          // if no pods are running, inference service is stopped, created or creating
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
    if (deploymentStatus == null) {
      // but the spec is not created, the deployment is still starting
      return new Pair<>(
        ServingStatusEnum.STARTING,
        ServingStatusCondition.getScheduledInProgressCondition()
      );
    }
  
    Boolean artifactFileExists = kubeArtifactUtils.checkArtifactFileExists(serving);
    Optional<DeploymentCondition> givenCondition = deploymentStatus.getConditions().stream()
      .filter(c -> c.getType().equals("ReplicaFailure")).findFirst();
    if (givenCondition.isPresent() && givenCondition.get().getStatus().equals("True")) {
      // if the given deployment condition is replica failure, the deployment failed
      ServingStatusCondition condition = kubeServingUtils.getDeploymentCondition(serving.getDeployed(),
        artifactFileExists, pods);
      return artifactFileExists
        ? new Pair<>(ServingStatusEnum.FAILED, condition)
        : new Pair<>(ServingStatusEnum.CREATING, condition);
    }
    
    boolean anyRestartedContainer = pods.stream().anyMatch(p -> {
      if (p.getStatus().getContainerStatuses().isEmpty()) return false;
      ContainerStatus status = p.getStatus().getContainerStatuses().get(0);
      return status.getRestartCount() != null && status.getRestartCount() > 2;
    });
    if (anyRestartedContainer) {
      return artifactFileExists
        ? new Pair<>(ServingStatusEnum.FAILED, ServingStatusCondition.getStartedFailedCondition(
        "predictor" + kubeServingUtils.STARTED_FAILED_CONDITION_MESSAGE))
        : new Pair<>(ServingStatusEnum.CREATING, ServingStatusCondition.getStoppedCreatingCondition());
    }
  
    ServingStatusCondition condition = kubeServingUtils.getDeploymentCondition(serving.getDeployed(),
      artifactFileExists, pods);
    if (condition.getStatus() != null && !condition.getStatus()) {
      // if it's a failing condition, the deployment is failing to start
      return artifactFileExists
        ? new Pair<>(ServingStatusEnum.FAILED, condition)
        : new Pair<>(ServingStatusEnum.CREATING, condition);
    }
  
    // otherwise, it is starting, updating or running
    Integer availableReplicas = deploymentStatus.getAvailableReplicas();
    ServingStatusEnum servingStatus = ServingStatusEnum.RUNNING;
    if (availableReplicas == null || !availableReplicas.equals(serving.getInstances())) {
      // if there is a mismatch between the requested number of instances and the number actually active
      // in Kubernetes, and it's the 1st generation, the serving cluster is starting
      servingStatus = deploymentStatus.getObservedGeneration() == 1
        ? ServingStatusEnum.STARTING
        : ServingStatusEnum.UPDATING;
    }

    return new Pair<>(servingStatus, condition);
  }
  
  private List<Pod> getPods(Project project, Serving serving) {
    String servingId = String.valueOf(serving.getId());
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("model", servingId);
    return kubeClientService.getPods(project, labelMap);
  }
  
  private Deployment buildDeployment(Project project, Users user, Serving serving,
    KubePredictorServerUtils kubePredictorServerUtils)
    throws ServiceDiscoveryException, ApiKeyException {
    Deployment deployment = kubePredictorServerUtils.buildServingDeployment(project, user, serving);
    
    // Add service.hops.works labels
    addHopsworksServingLabels(deployment.getMetadata(), project, serving);
    addHopsworksServingLabels(deployment.getSpec().getTemplate().getMetadata(), project, serving);
    
    // Add node selectors
    Map<String, String> nodeSelectorLabels = kubeServingUtils.getServingNodeLabels();
    if (nodeSelectorLabels != null) {
      deployment.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelectorLabels);
    }
    
    // Add node tolerations
    List<Map<String, String>> nodeTolerations = kubeServingUtils.getServingNodeTolerations();
    if (nodeTolerations != null) {
      List<Toleration> tolerations = nodeTolerations.stream().map(nt -> {
        Toleration toleration = new TolerationBuilder()
          .withKey(nt.get("key"))
          .withOperator(nt.get("operator"))
          .withEffect(nt.get("effect")).build();
        if (nt.containsKey("value")) {
          toleration.setValue(nt.get("value"));
        }
        return toleration;
      }).collect(Collectors.toList());
      
      deployment.getSpec().getTemplate().getSpec().setTolerations(tolerations);
    }
    
    return deployment;
  }
  
  private Service buildService(Project project, Serving serving, KubePredictorServerUtils kubePredictorServerUtils) {
    Service service = kubePredictorServerUtils.buildServingService(serving);
    
    // Add service.hops.works labels
    addHopsworksServingLabels(service.getMetadata(), project, serving);
    
    return service;
  }
  
  private ObjectMeta getDeploymentMetadata(String servingId, KubePredictorServerUtils kubePredictorServerUtils) {
    return new ObjectMetaBuilder()
      .withName(getDeploymentName(servingId, kubePredictorServerUtils))
      .build();
  }
  
  private ObjectMeta getServiceMetadata(String servingId, KubePredictorServerUtils kubePredictorServerUtils) {
    return new ObjectMetaBuilder()
      .withName(getServiceName(servingId, kubePredictorServerUtils))
      .build();
  }
  
  private String getDeploymentName(String servingId, KubePredictorServerUtils kubePredictorServerUtils) {
    return kubePredictorServerUtils.getDeploymentName(servingId);
  }
  
  private String getServiceName(String servingId, KubePredictorServerUtils kubePredictorServerUtils) {
    return kubePredictorServerUtils.getServiceName(servingId);
  }
  
  private void addHopsworksServingLabels(ObjectMeta metadata, Project project, Serving serving) {
    Map<String, String> labels = metadata.getLabels();
    Map<String, String> servingLabels = kubeServingUtils.getHopsworksServingLabels(project, serving);
    
    if (labels == null) {
      metadata.setLabels(servingLabels);
    } else {
      labels.putAll(servingLabels);
    }
  }
}
