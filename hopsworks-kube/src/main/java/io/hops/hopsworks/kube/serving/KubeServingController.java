/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.KafkaServingHelper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.common.util.Settings;

import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.util.ServingCommands.START;
import static io.hops.hopsworks.common.serving.util.ServingCommands.STOP;

/**
 * Contains the common functionality between kubernetes serving controllers, the specific functionality is provided by
 * serving-type-specific controllers, e.g SkLearnServingController, servingController.
 */
@KubeStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeServingController implements ServingController {

  @EJB
  private Settings settings;
  @EJB
  private ServingFacade servingFacade;
  @EJB
  private KafkaServingHelper kafkaServingHelper;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeTfServingController kubeTfServingController;
  @EJB
  private KubeSkLearnServingController kubeSKLearnServingController;

  @Override
  public List<ServingWrapper> getServings(Project project)
      throws ServingException, KafkaException, CryptoPasswordNotFoundException {
    List<Serving> servingList = servingFacade.findForProject(project);

    List<ServingWrapper> servingWrapperList= new ArrayList<>();
    for (Serving serving : servingList) {
      servingWrapperList.add(getServingInternal(project, serving));
    }

    return servingWrapperList;
  }

  @Override
  public ServingWrapper getServing(Project project, Integer id)
      throws ServingException, KafkaException, CryptoPasswordNotFoundException {
    Serving serving = servingFacade.findByProjectAndId(project, id);
    if (serving == null) {
      return null;
    }

    return getServingInternal(project, serving);
  }

  @Override
  public void deleteServing(Project project, Integer id) throws ServingException {
    Serving serving = servingFacade.acquireLock(project, id);

    String servingIdStr = String.valueOf(serving.getId());
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr, serving.getServingType()));

      // If pods are currently running for this serving instance, kill them
      if (deploymentStatus != null) {
        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingIdStr, serving.getServingType()));
      }

      Service serviceInfo = kubeClientService.getServiceInfo(project,
          getServiceName(servingIdStr, serving.getServingType()));

      // if there is a service, delete it
      if (serviceInfo != null) {
        kubeClientService.deleteService(project, getServiceMetadata(servingIdStr, serving.getServingType()));
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.SEVERE, null, e.getMessage(), e);
    }

    // If the call to Kubernetes succeeded, then Kubernetes is taking care of terminating the pods.
    // Safe to remove the entry from the db
    servingFacade.delete(serving);
  }

  @Override
  public void deleteServings(Project project) throws ServingException {
    // Nothing to do here. This function is called when a project is deleted.
    // During the project deletion, the namespace is deleted. Kubernetes takes care of removing
    // pods, namespaces and services
    return;
  }

  @Override
  public void startOrStop(Project project, Users user, Integer servingId, ServingCommands command)
      throws ServingException {
    Serving serving = servingFacade.acquireLock(project, servingId);

    String servingIdStr = String.valueOf(serving.getId());
    try {
      DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr, serving.getServingType()));

      Service instanceService = kubeClientService.getServiceInfo(project,
          getServiceName(servingIdStr, serving.getServingType()));

      Map<String, String> labelMap = new HashMap<>();
      labelMap.put("model", servingIdStr);
      List<Pod> podList = kubeClientService.getPodList(project, labelMap);

      ServingStatusEnum status = getInstanceStatus(serving, deploymentStatus,
          instanceService, podList);


      if ((status == ServingStatusEnum.RUNNING ||
          status == ServingStatusEnum.STARTING ||
          // Maybe something went wrong during the first stopping, give the opportunity to the user to fix it.
          status == ServingStatusEnum.STOPPING ||
          status == ServingStatusEnum.UPDATING) && command == STOP) {

        kubeClientService.deleteDeployment(project, getDeploymentMetadata(servingIdStr, serving.getServingType()));
        kubeClientService.deleteService(project, getDeploymentMetadata(servingIdStr, serving.getServingType()));

      } else if (status == ServingStatusEnum.STOPPED && command == START) {

        kubeClientService.createOrReplaceDeployment(project, buildServingDeployment(project, user, serving));
        kubeClientService.createOrReplaceService(project, buildServingService(serving));

      } else {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.FINE,
            "Instance is already: " + status.toString());
      }
    } catch (KubernetesClientException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE,
          null, e.getMessage(), e);
    } finally {
      servingFacade.releaseLock(project, servingId);
    }
  }

  @Override
  public void createOrUpdate(Project project, Users user, ServingWrapper newServing)
    throws KafkaException, UserException, ProjectException, ServiceException, ServingException, ExecutionException,
    InterruptedException {
    Serving serving = newServing.getServing();

    if (serving.getId() == null) {
      // Create request
      serving.setCreated(new Date());
      serving.setCreator(user);
      serving.setProject(project);

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newServing, serving, null);

      servingFacade.merge(serving);
    } else {
      Serving oldDbTfServing = servingFacade.acquireLock(project, serving.getId());

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newServing, serving, oldDbTfServing);

      // Update the object in the database
      Serving dbServing = servingFacade.updateDbObject(serving, project);

      String servingIdStr = String.valueOf(dbServing.getId());
      // If pods are currently running for this serving instance, submit a new deployment to update them
      try {
        DeploymentStatus deploymentStatus = kubeClientService.getDeploymentStatus(project,
            getDeploymentName(servingIdStr, dbServing.getServingType()));
        if (deploymentStatus != null) {
          kubeClientService.createOrReplaceDeployment(project,
              buildServingDeployment(project, user, dbServing));
        }
      } catch (KubernetesClientException e) {
        throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE, null, e.getMessage(), e);
      } finally {
        servingFacade.releaseLock(project, serving.getId());
      }
    }
  }

  @Override
  public int getMaxNumInstances() {
    return settings.getKubeMaxServingInstances();
  }

  @Override
  public String getClassName() {
    return KubeServingController.class.getName();
  }

  private ServingWrapper getServingInternal(Project project, Serving serving)
      throws ServingException {
    ServingWrapper servingWrapper = new ServingWrapper(serving);

    String servingIdStr = String.valueOf(serving.getId());

    DeploymentStatus deploymentStatus = null;
    Service instanceService = null;
    List<Pod> podList = null;
    try {
      deploymentStatus = kubeClientService.getDeploymentStatus(project,
          getDeploymentName(servingIdStr, serving.getServingType()));
      instanceService = kubeClientService.getServiceInfo(project,
          getServiceName(servingIdStr, serving.getServingType()));

      Map<String, String> labelMap = new HashMap<>();
      labelMap.put("model", servingIdStr);
      podList = kubeClientService.getPodList(project, labelMap);
    } catch (KubernetesClientException ex) {
      throw new ServingException(RESTCodes.ServingErrorCode.STATUSERROR, Level.SEVERE, null, ex.getMessage(), ex);
    }

    ServingStatusEnum status = getInstanceStatus(serving, deploymentStatus,
        instanceService, podList);

    servingWrapper.setStatus(status);

    switch (status) {
      case STARTING:
      case RUNNING:
      case UPDATING:
        servingWrapper.setNodePort(instanceService.getSpec().getPorts().get(0).getNodePort());
        servingWrapper.setAvailableReplicas(deploymentStatus.getAvailableReplicas() == null ? 0
            : deploymentStatus.getAvailableReplicas() );
        break;
      default:
        servingWrapper.setNodePort(null);
        servingWrapper.setAvailableReplicas(0);
    }

    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));

    return servingWrapper;
  }

  private ServingStatusEnum getInstanceStatus(Serving serving, DeploymentStatus deploymentStatus,
                                                Service instanceService, List<Pod> podList) {

    if (deploymentStatus != null && instanceService != null) {
      Integer availableReplicas = deploymentStatus.getAvailableReplicas();

      if (availableReplicas == null ||
          (!availableReplicas.equals(serving.getInstances()) && deploymentStatus.getObservedGeneration() == 1)) {
        // if there is a mismatch between the requested number of instances and the number actually active
        // in Kubernetes, and it's the 1st generation, the serving cluster is starting
        return  ServingStatusEnum.STARTING;
      } else if (availableReplicas.equals(serving.getInstances())) {
        return  ServingStatusEnum.RUNNING;
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
        return kubeSKLearnServingController.getDeploymentName(servingId);
    }

    return null;
  }

  private String getServiceName(String servingId, ServingType servingType) {
    switch (servingType) {
      case TENSORFLOW:
        return kubeTfServingController.getServiceName(servingId);
      case SKLEARN:
        return kubeSKLearnServingController.getServiceName(servingId);
    }

    return null;
  }

  private Deployment buildServingDeployment(Project project, Users user, Serving serving) {
    switch (serving.getServingType()) {
      case TENSORFLOW:
        return kubeTfServingController.buildServingDeployment(project, user, serving);
      case SKLEARN:
        return kubeSKLearnServingController.buildServingDeployment(project, user, serving);
    }

    return null;
  }

  private Service buildServingService(Serving serving) {
    switch (serving.getServingType()) {
      case TENSORFLOW:
        return kubeTfServingController.buildServingService(serving);
      case SKLEARN:
        return kubeSKLearnServingController.buildServingService(serving);
    }

    return null;
  }
}
