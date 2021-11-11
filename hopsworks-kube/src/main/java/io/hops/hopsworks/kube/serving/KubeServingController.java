/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.KafkaServingHelper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTransformerUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
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
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  
  @Override
  public List<ServingWrapper> getServings(Project project, String modelNameFilter, ServingStatusEnum statusFilter)
    throws ServingException {
    List<Serving> servingList;
    if(Strings.isNullOrEmpty(modelNameFilter)) {
      servingList = servingFacade.findForProject(project);
    } else {
      servingList = servingFacade.findForProjectAndModel(project, modelNameFilter);
    }

    List<ServingWrapper> servingWrapperList = new ArrayList<>();
    for (Serving serving : servingList) {
      ServingWrapper servingWrapper = getServingInternal(project, serving);
      // If status filter is set only add servings with the defined status
      if(statusFilter != null && !servingWrapper.getStatus().name().equals(statusFilter.name())) {
        continue;
      }
      servingWrapperList.add(servingWrapper);
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
    
    try {
      if (serving.getDeployed() != null) {
        // If the serving is deployed, check status before deleting it
        KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
        ServingStatusEnum status = internalStatus.getServingStatus();
        
        if (!internalStatus.getAvailable() && status == ServingStatusEnum.STARTING) {
          // If the serving is starting but we can't get the nº of available replicas, the inference service is not
          // created yet. Therefore, we cannot delete it since it is still not materialized in the api server.
          // Checking the available replicas for the predictor is enough. Ignore transformer.
          throw new ServingException(RESTCodes.ServingErrorCode.DELETIONERROR, Level.FINE,
            "Instance is busy. Please, try later.");
        }
      }
    } finally {
      servingFacade.releaseLock(project, id);
    }
    
    // Otherwise, delete the instance
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
          String newRevision = kubeServingUtils.getNewRevisionID();
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
          if (!internalStatus.getAvailable() && status == ServingStatusEnum.STARTING) {
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
  public void createOrUpdate(Project project, Users user, ServingWrapper servingWrapper)
    throws KafkaException, UserException, ProjectException, ServingException, ExecutionException,
    InterruptedException {
    Serving serving = servingWrapper.getServing();
    
    try {
      // Ensure artifacts root directory exists
      kubeArtifactUtils.createArtifactsRootDir(project, user, serving);
  
      if (serving.getArtifactVersion() == null || serving.getArtifactVersion() == -1) {
        // If the artifact version is greater than 0, the artifact already exists.
        // Otherwise, a version value of -1 or null will create a new artifact.
        // Shared artifacts are assigned version 0, containing only the model files.
        Integer version = !kubeArtifactUtils.isSharedArtifact(serving)
          ? kubeArtifactUtils.getNextArtifactVersion(serving)
          : 0;
        serving.setArtifactVersion(version);
        Boolean created = kubeArtifactUtils.createArtifact(project, user, serving);
        if (created && serving.getTransformer() != null) {
          // Update transformer name
          serving.setTransformer(kubeTransformerUtils.getTransformerFileName(serving, false));
        }
      } else {
        // Otherwise, check if the artifact version folder exists
        if (!kubeArtifactUtils.checkArtifactDirExists(serving)) {
          throw new IllegalArgumentException("Artifact with version " + serving.getArtifactVersion().toString() +
            " does not exist");
        }
        // Verify that the selected transformer file is available in the artifact version folder. When updating a
        // serving the user could have changed the transformer script path
        if (serving.getArtifactVersion() > 0 && !kubeTransformerUtils.checkTransformerExists(serving)) {
          throw new IllegalArgumentException("Transformer script cannot change in an existent artifact");
        }
      }
    } catch (DatasetException | HopsSecurityException | ServiceException | IOException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO, "Artifact could not be " +
        "created or verified", e.getMessage(), e);
    }
    
    if (serving.getId() == null) {
      // Create request
      serving.setCreated(new Date());
      serving.setCreator(user);
      serving.setProject(project);
      
      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, null);
      
      servingFacade.merge(serving);
    } else {
      Serving oldServing = servingFacade.acquireLock(project, serving.getId());
      KubeToolServingController toolServingController = getServingController(oldServing);
      
      // If artifact already exists, check the transformer script is not modified
      if (oldServing.getArtifactVersion() > 0 && oldServing.getArtifactVersion() == serving.getArtifactVersion()
        && !oldServing.getTransformer().equals(serving.getTransformer())) {
        throw new IllegalArgumentException("Transformer script cannot change in an existent artifact");
      }
      
      // Set missing fields to keep consistency with the stored serving entity
      serving = servingFacade.fill(serving, oldServing);
      
      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, oldServing);
      
      try {
        // Avoid updating if there are no changes
        if (oldServing.equals(serving)) {
          return;
        }
        
        // Merge serving fields
        Serving newServing = servingFacade.mergeServings(oldServing, serving);
        
        KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, newServing);
        ServingStatusEnum status = internalStatus.getServingStatus();
        if (newServing.getDeployed() != null) {
          // If the serving is deployed, check the serving status
          if (!internalStatus.getAvailable() && status == ServingStatusEnum.STARTING) {
            // If the serving is starting but we can't get the nº of available replicas, the inference service is not
            // created yet. Therefore, we cannot update it since it cannot be found.
            throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.FINE,
              "Instance is busy. Please, try later.");
          }
          if (status == ServingStatusEnum.RUNNING) {
            // Generate pseudo-random revision id
            String newRevision = kubeServingUtils.getNewRevisionID();
            newServing.setRevision(newRevision);
            // If serving name or serving tool change, delete old instance and create the new one
            // Otherwise, update current instance
            if (!newServing.getName().equals(oldServing.getName()) ||
              newServing.getServingTool() != oldServing.getServingTool()) {
              toolServingController.deleteInstance(project, oldServing);
              getServingController(newServing).createInstance(project, user, newServing);
            } else {
              toolServingController.updateInstance(project, user, newServing);
            }
          } else if (status == ServingStatusEnum.STARTING || status == ServingStatusEnum.UPDATING) {
            // If the serving is already starting or updating, applying an additional update can overload the node with
            // idle terminating pods.
            throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.FINE,
              "Instance is already updating. Please, try later.");
          }
        }
        // Update the object in the database
        servingFacade.updateDbObject(newServing, project);
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
    servingWrapper.setAvailableTransformerReplicas(internalStatus.getAvailableTransformerReplicas());
    servingWrapper.setConditions(internalStatus.getConditions());
    
    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));
    
    return servingWrapper;
  }
  
  private KubeToolServingController getServingController(Serving serving) {
    return serving.getServingTool() == ServingTool.KFSERVING
      ? kubeKfServingController
      : kubeDeploymentServingController;
  }
}
