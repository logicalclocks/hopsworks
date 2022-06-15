/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubePredictorServerUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

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
public class KubeDeploymentServingController extends KubeToolServingController {
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeServingUtils kubeServingUtils;
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
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null, e.getMessage(), e);
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
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
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
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    KubePredictorServerUtils kubePredictorServerUtils = kubePredictorUtils.getPredictorServerUtils(serving);
    
    DeploymentStatus deploymentStatus;
    List<Pod> podList;
    Service instanceService;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId,
        kubePredictorServerUtils));
      podList = getPodList(project, serving);
      instanceService = kubeClientService.getServiceInfo(project, getServiceName(servingId, kubePredictorServerUtils));
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE,
        "Error while getting service status", e.getMessage(), e);
    }

    ServingStatusEnum status = getServingStatus(serving, deploymentStatus, podList);
    Integer nodePort = instanceService == null ? null : instanceService.getSpec().getPorts().get(0).getNodePort();
    Integer availableReplicas = kubeServingUtils.getAvailableReplicas(deploymentStatus);
    List<String> conditions = deploymentStatus == null ? new ArrayList<>() : deploymentStatus.getConditions().stream()
      .filter(c -> c.getType().equals("Available") && c.getStatus().equals("False"))
      .map(c -> String.format("Predictor: %s - %s", c.getReason(), c.getMessage()))
      .collect(Collectors.toList());
    
    return new KubeServingInternalStatus() {
      {
        setServingStatus(status);
        setAvailable(deploymentStatus != null && instanceService != null);
        setAvailableReplicas(availableReplicas);
        setConditions(conditions.size() > 0 ? conditions : null);
        setModelServerInferencePath(kubeServingUtils.getModelServerInferencePath(serving, null));
        setHopsworksInferencePath(kubeServingUtils.getHopsworksInferencePath(serving, null));
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
  
  private List<Pod> getPodList(Project project, Serving serving) {
    String servingId = String.valueOf(serving.getId());
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("model", servingId);
    return kubeClientService.getPodList(project, labelMap);
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
