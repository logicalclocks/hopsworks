/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.KafkaServingHelper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.util.ServingCommands.START;
import static io.hops.hopsworks.common.serving.util.ServingCommands.STOP;

/**
 * Contains the common functionality between kubernetes serving controllers, the specific functionality is provided by
 * tool-specific controllers (e.g DeploymentServingController, KfServingServingController) and serving-type-specific
 * controllers (e.g SkLearnServingController, TfServingController).
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
  private KubeKfServingController kubeKfServingController;
  @EJB
  private KubeDeploymentServingController kubeDeploymentServingController;

  @Override
  public List<ServingWrapper> getServings(Project project)
      throws ServingException {
    List<Serving> servingList = servingFacade.findForProject(project);

    List<ServingWrapper> servingWrapperList= new ArrayList<>();
    for (Serving serving : servingList) {
      servingWrapperList.add(getServingInternal(project, serving));
    }

    return servingWrapperList;
  }

  @Override
  public ServingWrapper getServing(Project project, Integer id)
      throws ServingException {
    Serving serving = servingFacade.findByProjectAndId(project, id);
    if (serving == null) {
      return null;
    }

    return getServingInternal(project, serving);
  }

  @Override
  public void deleteServing(Project project, Integer id) throws ServingException {
    Serving serving = servingFacade.acquireLock(project, id);

    KubeToolServingController toolServingController = getServingController(serving);
    toolServingController.deleteInstance(project, serving);

    // If the call to Kubernetes succeeded, then Kubernetes is taking care of terminating the pods.
    // Safe to remove the entry from the db
    servingFacade.delete(serving);
  }

  @Override
  public void deleteServings(Project project) {
    // Nothing to do here. This function is called when a project is deleted.
    // During the project deletion, the namespace is deleted. Kubernetes takes care of removing
    // pods, namespaces and services
    return;
  }

  @Override
  public void startOrStop(Project project, Users user, Integer servingId, ServingCommands command)
      throws ServingException {
    Serving serving = servingFacade.acquireLock(project, servingId);
    KubeToolServingController toolServingController = getServingController(serving);

    KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
    ServingStatusEnum status = internalStatus.getServingStatus();
    
    try {
      if ((status == ServingStatusEnum.RUNNING ||
          status == ServingStatusEnum.STARTING ||
          // Maybe something went wrong during the first stopping, give the opportunity to the user to fix it.
          status == ServingStatusEnum.STOPPING ||
          status == ServingStatusEnum.UPDATING) && command == STOP) {

        // Delete instance
        toolServingController.deleteInstance(project, serving);

      } else if (status == ServingStatusEnum.STOPPED && command == START) {

        // Create instance
        toolServingController.createInstance(project, user, serving);

      } else {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.FINE,
            "Instance is already: " + status.toString());
      }
    } finally {
      servingFacade.releaseLock(project, servingId);
    }
  }

  @Override
  public void createOrUpdate(Project project, Users user, ServingWrapper newServing)
    throws KafkaException, UserException, ProjectException, ServingException, ExecutionException,
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
      Serving oldDbServing = servingFacade.acquireLock(project, serving.getId());
      KubeToolServingController toolServingController = getServingController(oldDbServing);

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newServing, serving, oldDbServing);

      // Update the object in the database
      Serving dbServing = servingFacade.updateDbObject(serving, project);

      // If pods are currently running for this serving instance, submit a new deployment to update them
      try {
        ServingStatusEnum status = toolServingController.getInternalStatus(project, serving).getServingStatus();
        if (status == ServingStatusEnum.STARTING || status == ServingStatusEnum.RUNNING ||
          status == ServingStatusEnum.UPDATING) {
          // If serving tool doesn't change, update current instance.
          // Otherwise, delete old instance and create the new one.
          if (serving.getServingTool() == oldDbServing.getServingTool()) {
            toolServingController.updateInstance(project, user, dbServing);
          } else {
            KubeToolServingController newToolServingController = getServingController(serving);
            toolServingController.deleteInstance(project, oldDbServing);
            newToolServingController.createInstance(project, user, serving);
          }
        }
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

    KubeToolServingController toolServingController = getServingController(serving);
    KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);

    servingWrapper.setStatus(internalStatus.getServingStatus());
    servingWrapper.setNodePort(internalStatus.getNodePort());
    servingWrapper.setAvailableReplicas(internalStatus.getAvailableReplicas());

    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));

    return servingWrapper;
  }

  private KubeToolServingController getServingController(Serving serving) {
    return serving.getServingTool() == ServingTool.KFSERVING
      ? kubeKfServingController
      : kubeDeploymentServingController;
  }
}
