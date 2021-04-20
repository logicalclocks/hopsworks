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
import io.hops.hopsworks.kube.common.KubeServingUtils;
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
      if (command == START) {
        if (serving.getDeployed() == null || status == ServingStatusEnum.STOPPED
          || status == ServingStatusEnum.STOPPING) {
          // If the serving is not deployed, create a new instance
          String newRevision = KubeServingUtils.getNewRevisionID();
          serving.setRevision(newRevision);
          toolServingController.createInstance(project, user, serving);
          serving.setDeployed(new Date());
          servingFacade.updateDbObject(serving, project);
        } else {
          // Otherwise, an instance has already been created
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.FINE,
            "Instance is already " + status.toString().toLowerCase());
        }
      }
      
      if (command == STOP) {
        if (serving.getDeployed() != null) {
          // If the serving is deployed, check the serving status
          if (internalStatus.getAvailableReplicas() == null && status == ServingStatusEnum.STARTING) {
            // If the serving is starting but we can't get the nº of available replicas, the inference service is not
            // created yet. Therefore, we cannot stop it since it cannot be found.
            throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.FINE,
              "Instance is busy. Please, try later.");
          }
          if (status != ServingStatusEnum.STOPPED) {
            // If serving is deployed and the inference service can be found, delete it
            toolServingController.deleteInstance(project, serving);
            serving.setDeployed(null);
            servingFacade.updateDbObject(serving, project);
          }
        } else {
          // Otherwise, the instance is already stopped or stopping
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.FINE,
            "Instance is already " + status.toString().toLowerCase());
        }
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

      // Set deployed timestamp and revision to keep consistency with the stored serving entity
      serving.setDeployed(oldDbServing.getDeployed());
      serving.setRevision(oldDbServing.getRevision());
      
      // Merge serving fields
      Serving dbServing = servingFacade.mergeServings(oldDbServing, serving);

      // If pods are currently running for this serving instance, submit a new deployment to update them
      try {
        KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, dbServing);
        ServingStatusEnum status = internalStatus.getServingStatus();
        if (dbServing.getDeployed() != null) {
          // If the serving is deployed, check the serving status
          if (internalStatus.getAvailableReplicas() == null && status == ServingStatusEnum.STARTING) {
            // If the serving is starting but we can't get the nº of available replicas, the inference service is not
            // created yet. Therefore, we cannot update it since it cannot be found.
            throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.FINE,
              "Instance is busy. Please, try later.");
          }
          if (status == ServingStatusEnum.RUNNING) {
            // Generate pseudo-random revision id
            String newRevision = KubeServingUtils.getNewRevisionID();
            dbServing.setRevision(newRevision);
            // If serving name or serving tool change, delete old instance and create the new one
            // Otherwise, update current instance
            if (!dbServing.getName().equals(oldDbServing.getName()) ||
              dbServing.getServingTool() != oldDbServing.getServingTool()) {
              toolServingController.deleteInstance(project, oldDbServing);
              getServingController(dbServing).createInstance(project, user, dbServing);
            } else {
              toolServingController.updateInstance(project, user, dbServing);
            }
          } else if (status == ServingStatusEnum.STARTING || status == ServingStatusEnum.UPDATING) {
            // If the serving is already starting or updating, applying an additional update can overload the node with
            // idle terminating pods.
            throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.FINE,
              "Instance is already updating. Please, try later.");
          }
        }
        // Setup the Kafka topic for logging
        kafkaServingHelper.setupKafkaServingTopic(project, newServing, dbServing, oldDbServing);
        // Update the object in the database
        servingFacade.updateDbObject(dbServing, project);
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
