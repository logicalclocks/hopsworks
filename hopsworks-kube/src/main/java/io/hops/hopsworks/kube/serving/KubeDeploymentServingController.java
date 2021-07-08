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
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeSkLearnServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTfServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
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
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeTfServingUtils kubeTfServingUtils;
  @EJB
  private KubeSkLearnServingUtils kubeSkLearnServingUtils;
  
  @Override
  public void createInstance(Project project, Users user, Serving serving) throws ServingException {
    try {
      kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving));
      kubeClientService.createOrReplaceService(project, buildService(project, serving));
    } catch (ServiceDiscoveryException | ServiceException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void updateInstance(Project project, Users user, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    try {
      DeploymentStatus deploymentStatus =
        kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId, serving.getModelServer()));
      if (deploymentStatus != null) {
        kubeClientService.createOrReplaceDeployment(project, buildDeployment(project, user, serving));
      }
    } catch (KubernetesClientException | ServiceDiscoveryException | ServiceException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteInstance(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
        getDeploymentName(servingId, serving.getModelServer()));
      
      // If pods are currently running for this serving instance, kill them
      if (deploymentStatus != null) {
        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingId, serving.getModelServer()));
      }
      
      Service serviceInfo = kubeClientService.getServiceInfo(project, getServiceName(servingId,
        serving.getModelServer()));
      
      // if there is a service, delete it
      if (serviceInfo != null) {
        kubeClientService.deleteService(project, getServiceMetadata(servingId, serving.getModelServer()));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }
  
  @Override
  public KubeServingInternalStatus getInternalStatus(Project project, Serving serving) throws ServingException {
    String servingId = String.valueOf(serving.getId());
    ModelServer modelServer = serving.getModelServer();
    
    DeploymentStatus deploymentStatus;
    List<Pod> podList;
    Service instanceService;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project, getDeploymentName(servingId, modelServer));
      podList = getPodList(project, serving);
      instanceService = kubeClientService.getServiceInfo(project, getServiceName(servingId, modelServer));
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE,
        "Error while getting service status", e.getMessage(), e);
    }

    ServingStatusEnum status = getServingStatus(serving, deploymentStatus, podList);
    Integer nodePort = instanceService == null ? null : instanceService.getSpec().getPorts().get(0).getNodePort();
    Integer availableReplicas = kubeServingUtils.getAvailableReplicas(deploymentStatus);
    return new KubeServingInternalStatus() {
      {
        setServingStatus(status);
        setNodePort(nodePort);
        setAvailable(deploymentStatus != null && instanceService != null);
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
  
  private Deployment buildDeployment(Project project, Users user, Serving serving)
    throws ServiceDiscoveryException, ServiceException {
    Deployment deployment;
    switch (serving.getModelServer()) {
      case TENSORFLOW_SERVING:
        deployment = kubeTfServingUtils.buildServingDeployment(project, user, serving);
        break;
      case FLASK:
        deployment = kubeSkLearnServingUtils.buildServingDeployment(project, user, serving);
        break;
      default:
        throw new NotSupportedException("Model server not supported for kubernetes deployments");
    }
    
    // Add service.hops.works labels
    addHopsworksServingLabels(deployment.getMetadata(), project, serving);
    addHopsworksServingLabels(deployment.getSpec().getTemplate().getMetadata(), project, serving);
    
    return deployment;
  }
  
  private Service buildService(Project project, Serving serving) {
    Service service;
    switch (serving.getModelServer()) {
      case TENSORFLOW_SERVING:
        service = kubeTfServingUtils.buildServingService(serving);
        break;
      case FLASK:
        service = kubeSkLearnServingUtils.buildServingService(serving);
        break;
      default:
        throw new NotSupportedException("Model server not supported for kubernetes services");
    }
    
    // Add service.hops.works labels
    addHopsworksServingLabels(service.getMetadata(), project, serving);
    
    return service;
  }
  
  private ObjectMeta getDeploymentMetadata(String servingId, ModelServer modelServer) {
    return new ObjectMetaBuilder()
      .withName(getDeploymentName(servingId, modelServer))
      .build();
  }
  
  private ObjectMeta getServiceMetadata(String servingId, ModelServer modelServer) {
    return new ObjectMetaBuilder()
      .withName(getServiceName(servingId, modelServer))
      .build();
  }
  
  private String getDeploymentName(String servingId, ModelServer modelServer) {
    switch (modelServer) {
      case TENSORFLOW_SERVING:
        return kubeTfServingUtils.getDeploymentName(servingId);
      case FLASK:
        return kubeSkLearnServingUtils.getDeploymentName(servingId);
      default:
        throw new NotSupportedException("Model server not supported for kubernetes deployments");
    }
  }
  
  private String getServiceName(String servingId, ModelServer modelServer) {
    switch (modelServer) {
      case TENSORFLOW_SERVING:
        return kubeTfServingUtils.getServiceName(servingId);
      case FLASK:
        return kubeSkLearnServingUtils.getServiceName(servingId);
      default:
        throw new NotSupportedException("Model server not supported for kubernetes services");
    }
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
