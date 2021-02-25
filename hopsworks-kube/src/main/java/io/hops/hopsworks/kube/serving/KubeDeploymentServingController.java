/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.NotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeDeploymentServingController extends KubeToolServingController {
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeTfServingController kubeTfServingController;
  @EJB
  private KubeSkLearnServingController kubeSkLearnServingController;
  
  public KubeDeploymentServingController() {
    super("deployment");
  }
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving));
      kubeClientService.createOrReplaceService(project, buildService(project, serving));
    } catch (ServiceDiscoveryException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    try {
      DeploymentStatus deploymentStatus =
        kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId, serving.getServingType()));
      if (deploymentStatus != null) {
        kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving));
      }
    } catch (KubernetesClientException | ServiceDiscoveryException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
        getDeploymentName(servingId, serving.getServingType()));
      
      // If pods are currently running for this serving instance, kill them
      if (deploymentStatus != null) {
        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingId, serving.getServingType()));
      }
      
      Service serviceInfo = kubeClientService.getServiceInfo(project, getServiceName(servingId,
        serving.getServingType()));
      
      // if there is a service, delete it
      if (serviceInfo != null) {
        kubeClientService.deleteService(project, getServiceMetadata(servingId, serving.getServingType()));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    ServingType servingType = serving.getServingType();
    
    DeploymentStatus deploymentStatus;
    List<Pod> podList;
    Service instanceService;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId, servingType));
      podList = getPodList(project, serving);
      instanceService = kubeClientService.getServiceInfo(project, getServiceName(servingId, servingType));
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE,
        "Error while getting service status", e.getMessage(), e);
    }

    ServingStatusEnum status = getServingStatus(serving, deploymentStatus, podList);
    Integer nodePort = instanceService == null ? null : instanceService.getSpec().getPorts().get(0).getNodePort();
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
  
  private List<Pod> getPodList(Project project, Serving serving) {
    String servingId = String.valueOf(serving.getId());
    Map<String, String> labelMap = new HashMap<>();
    labelMap.put("model", servingId);
    return kubeClientService.getPodList(project, labelMap);
  }
  
  private Deployment buildDeployment(Project project, Users user, Serving serving) throws ServiceDiscoveryException {
    Deployment deployment;
    switch (serving.getServingType()) {
      case TENSORFLOW:
        deployment = kubeTfServingController.buildServingDeployment(project, user, serving);
        break;
      case SKLEARN:
        deployment = kubeSkLearnServingController.buildServingDeployment(project, user, serving);
        break;
      default:
        throw new NotSupportedException("Serving type not supported for kubernetes deployments");
    }
    
    // Add service.hops.works labels
    addHopsworksServingLabels(deployment.getMetadata(), project, serving);
    // TODO: Add labels to pods included in the deployment
    addHopsworksServingLabels(deployment.getSpec().getTemplate().getMetadata(), project, serving);
    
    return deployment;
  }
  
  private Service buildService(Project project, Serving serving) {
    Service service;
    switch (serving.getServingType()) {
      case TENSORFLOW:
        service = kubeTfServingController.buildServingService(serving);
        break;
      case SKLEARN:
        service = kubeSkLearnServingController.buildServingService(serving);
        break;
      default:
        throw new NotSupportedException("Serving type not supported for kubernetes services");
    }
    
    // Add service.hops.works labels
    addHopsworksServingLabels(service.getMetadata(), project, serving);
    
    return service;
  }
  
  private ObjectMeta getDeploymentMetadata(String servingId, ServingType servingType) {
    return new ObjectMetaBuilder()
      .withName(getDeploymentName(servingId, servingType))
      .build();
  }
  
  private ObjectMeta getServiceMetadata(String servingId, ServingType servingType) {
    return new ObjectMetaBuilder()
      .withName(getServiceName(servingId, servingType))
      .build();
  }
  
  private String getDeploymentName(String servingId, ServingType servingType) {
    switch (servingType) {
      case TENSORFLOW:
        return kubeTfServingController.getDeploymentName(servingId);
      case SKLEARN:
        return kubeSkLearnServingController.getDeploymentName(servingId);
      default:
        throw new NotSupportedException("Serving type not supported for kubernetes deployments");
    }
  }
  
  private String getServiceName(String servingId, ServingType servingType) {
    switch (servingType) {
      case TENSORFLOW:
        return kubeTfServingController.getServiceName(servingId);
      case SKLEARN:
        return kubeSkLearnServingController.getServiceName(servingId);
      default:
        throw new NotSupportedException("Serving type not supported for kubernetes services");
    }
  }
  
  private void addHopsworksServingLabels(ObjectMeta metadata, Project project, Serving serving) {
    String servingId = String.valueOf(serving.getId());
    Integer projectId = project.getId();
    ServingType servingType = serving.getServingType();
    
    Map<String, String> labels = metadata.getLabels();
    Map<String, String> servingLabels = KubeServingUtils.getHopsworksServingLabels(projectId, servingId,
      serving.getName(), KubeServingUtils.getModelName(serving), serving.getVersion(), servingType, SERVING_TOOL_NAME);
    
    if (labels == null) {
      metadata.setLabels(servingLabels);
    } else {
      labels.putAll(servingLabels);
    }
  }
}
