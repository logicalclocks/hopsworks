/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingLogs;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.KafkaServingHelper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.common.serving.util.ServingUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.serving.utils.KubeArtifactUtils;
import io.hops.hopsworks.kube.serving.utils.KubePredictorUtils;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.kube.serving.utils.KubeTransformerUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.BatchingConfiguration;
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.util.ServingCommands.START;
import static io.hops.hopsworks.common.serving.util.ServingCommands.STOP;

/**
 * Contains the common functionality between kubernetes serving controllers, the specific functionality is provided by
 * tool-specific controllers (e.g DeploymentServingController, KServeServingController) and serving-type-specific
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
  private KubeKServeController kubeKServeController;
  @EJB
  private KubeDeploymentServingController kubeDeploymentServingController;
  @EJB
  private ServingUtils servingUtils;
  @EJB
  private KubeServingUtils kubeServingUtils;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  
  @Override
  public List<ServingWrapper> getAll(Project project, String modelNameFilter, Integer modelVersionFilter,
    ServingStatusEnum statusFilter)
      throws ServingException {
    List<Serving> servingList;
    if (Strings.isNullOrEmpty(modelNameFilter)) {
      servingList = servingFacade.findForProject(project);
    } else if (modelVersionFilter == null) {
      servingList = servingFacade.findForProjectAndModel(project, modelNameFilter);
    } else {
      servingList = servingFacade.findForProjectAndModelVersion(project, modelNameFilter, modelVersionFilter);
    }
  
    List<ServingWrapper> servingWrapperList = new ArrayList<>();
    for (Serving serving : servingList) {
      ServingWrapper servingWrapper = getServingInternal(project, serving);
      // If status filter is set only add servings with the defined status
      if (statusFilter != null && servingWrapper.getStatus() != statusFilter) {
        continue;
      }
      servingWrapperList.add(servingWrapper);
    }
    
    return servingWrapperList;
  }
  
  @Override
  public ServingWrapper get(Project project, Integer id)
    throws ServingException {
    Serving serving = servingFacade.findByProjectAndId(project, id);
    if (serving == null) {
      return null;
    }
    
    return getServingInternal(project, serving);
  }
  
  @Override
  public ServingWrapper get(Project project, String name)
    throws ServingException {
    Serving serving = servingFacade.findByProjectAndName(project, name);
    if (serving == null) {
      return null;
    }
    
    return getServingInternal(project, serving);
  }
  
  @Override
  public void delete(Project project, Integer id) throws ServingException {
    Serving serving = servingFacade.acquireLock(project, id);
    KubeToolServingController toolServingController = getServingController(serving);
    
    try {
      if (serving.getDeployed() != null) {
        // If the serving is deployed, check status before deleting it
        KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
        ServingStatusEnum status = internalStatus.getServingStatus();
        
        if (!internalStatus.getAvailable() ||
           status == ServingStatusEnum.STARTING || status == ServingStatusEnum.UPDATING) {
          // If the serving is starting/updating or we can't get the nº of available replicas, (inference
          // service not created yet), don't allow deletions.
          throw new ServingException(RESTCodes.ServingErrorCode.DELETION_ERROR, Level.FINE,
            "Deployment is starting. Please, try later.");
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
  public void deleteAll(Project project) {
    // Nothing to do here. This function is called when a project is deleted.
    // During the project deletion, the namespace is deleted. Kubernetes takes care of removing
    // pods, namespaces and services
  }
  
  @Override
  public void startOrStop(Project project, Users user, Integer servingId, ServingCommands command)
    throws ServingException {
    Serving serving = servingFacade.acquireLock(project, servingId); // lock serving
    KubeToolServingController toolServingController = getServingController(serving);
    
    try {
      KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
      if (command == START) {
        start(project, user, serving, internalStatus);
      } else if  (command == STOP) {
        stop(project, serving, internalStatus);
      }
    } finally {
      servingFacade.releaseLock(project, servingId); // unlock serving
    }
  }
  
  @Override
  public void put(Project project, Users user, ServingWrapper servingWrapper)
    throws KafkaException, UserException, ProjectException, ServingException, ExecutionException,
    InterruptedException {

    // Create model artifact if it doesn't exist
    prepareModelArtifact(project, user, servingWrapper);
    
    if (servingWrapper.getServing().getId() == null) {
      createServing(project, user, servingWrapper);
    } else {
      updateServing(project, user, servingWrapper);
    }
  }
  
  @Override
  public List<ServingLogs> getLogs(Project project, Integer servingId, String component, Integer tailingLines)
    throws ServingException {
    Serving serving = servingFacade.acquireLock(project, servingId);
    KubeToolServingController toolServingController = getServingController(serving);
    
    try {
      KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
      
      if (internalStatus.getServingStatus() == ServingStatusEnum.CREATING ||
          internalStatus.getServingStatus() == ServingStatusEnum.CREATED ||
          internalStatus.getServingStatus() == ServingStatusEnum.IDLE ||
          internalStatus.getServingStatus() == ServingStatusEnum.STOPPED ||
          internalStatus.getServingStatus() == ServingStatusEnum.STOPPING) {
        throw new ServingException(RESTCodes.ServingErrorCode.SERVER_LOGS_NOT_AVAILABLE, Level.FINE);
      }
  
      if (component.equals("transformer") &&
        (serving.getServingTool() != ServingTool.KSERVE || serving.getTransformer() == null)) {
        throw new IllegalArgumentException("Transformer logs only available in KServe deployments with transformer");
      }
  
      return toolServingController.getLogs(project, serving, component, tailingLines);
    } finally {
      servingFacade.releaseLock(project, serving.getId());
    }
  }
  
  @Override
  public String getClassName() {
    return KubeServingController.class.getName();
  }
  
  private void start(Project project, Users user, Serving serving, KubeServingInternalStatus internalStatus)
     throws ServingException {
    switch (internalStatus.getServingStatus()) {
      case CREATING:
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is being prepared. Please, try later.");
      case CREATED:
      case STOPPED:
        if (serving.getDeployed() == null) {
          // if the serving is not deployed, create a new instance
          String newRevision = servingUtils.getNewRevisionID();
          serving.setRevision(newRevision);
          getServingController(serving).createInstance(project, user, serving);
          serving.setDeployed(new Date());
          servingFacade.updateDbObject(serving, project);
        }
        break;
      case STARTING:
      case RUNNING:
      case IDLE:
      case UPDATING:
        if (serving.getDeployed() == null) {
          // if not deployed, deployment is stopping
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is busy. Please, try later.");
        }
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is already started.");
      case STOPPING:
        if (serving.getDeployed() == null) {
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is stopping. Please, try later.");
        }
        // if deployed, deployment might be starting again
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is busy. Please, try later.");
      case FAILED:
        if (serving.getDeployed() == null) {
          // if not deployed, deployment is stopping
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is busy. Please, try later.");
        }
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is in a failed state and cannot be started.");
    }
  }
  
  private void stop(Project project, Serving serving, KubeServingInternalStatus internalStatus)
      throws ServingException {
    switch (internalStatus.getServingStatus()) {
      case CREATING:
        if (serving.getDeployed() == null) {
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is already stopped.");
        }
        break;
      case CREATED:
      case STOPPING:
      case STOPPED:
        if (serving.getDeployed() == null) {
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is already stopped.");
        }
        // if there's deployed time, deployment is starting
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is busy. Please, try later.");
      case STARTING:
        if (serving.getDeployed() == null) {
          // if not deployed, the deployment might be stopping already
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is busy. Please, try later.");
        }
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is starting. Please, try later.");
      case RUNNING:
      case FAILED:
      case IDLE:
        if (serving.getDeployed() != null) {
          // if the serving is deployed
          if (internalStatus.getAvailable()) {
            // and the inference service can be found, delete it
            getServingController(serving).deleteInstance(project, serving);
            serving.setDeployed(null);
            servingFacade.updateDbObject(serving, project);
          }
        }
        break;
      case UPDATING:
        if (serving.getDeployed() == null) {
          // if not deployed, the deployment might be stopping already
          throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
            "Deployment is busy. Please, try later.");
        }
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE,
          "Deployment is updating. Please, try later.");
    }
  }
  
  private void prepareModelArtifact(Project project, Users user, ServingWrapper servingWrapper)
      throws ServingException {
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
        boolean created = kubeArtifactUtils.createArtifact(project, user, serving);
        if (created) {
          if (serving.getPredictor() != null) { // Update predictor name
            serving.setPredictor(kubePredictorUtils.getPredictorFileName(serving, false));
          }
          if (serving.getTransformer() != null) { // Update transformer name
            serving.setTransformer(kubeTransformerUtils.getTransformerFileName(serving, false));
          }
        }
      } else {
        // Otherwise, check if the artifact version folder exists
        if (!kubeArtifactUtils.checkArtifactDirExists(serving)) {
          throw new IllegalArgumentException("Artifact with version " + serving.getArtifactVersion().toString() +
            " does not exist in model " + serving.getModelName() + " with version " + serving.getModelVersion());
        }
        // Verify that the selected assets are available in the artifact version folder. When updating a
        // serving the user could have changed the script paths
        if (serving.getArtifactVersion() > 0) {
          if (serving.getPredictor() != null && !kubePredictorUtils.checkPredictorExists(serving)) {
            throw new IllegalArgumentException("Predictor script cannot change in an existent artifact");
          }
          if (serving.getTransformer() != null && !kubeTransformerUtils.checkTransformerExists(serving)) {
            throw new IllegalArgumentException("Transformer script cannot change in an existent artifact");
          }
        }
      }
    } catch (DatasetException | HopsSecurityException | ServiceException | IOException e) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR_INT, Level.INFO, "Artifact could not be " +
        "created or verified", e.getMessage(), e);
    }
  }
  
  private void createServing(Project project, Users user, ServingWrapper servingWrapper)
    throws ProjectException, ServingException, KafkaException, UserException, ExecutionException, InterruptedException {
    Serving serving = servingWrapper.getServing();
    
    // New serving request
    serving.setCreated(new Date());
    serving.setCreator(user);
    serving.setProject(project);
  
    // Setup the Kafka topic for logging
    kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, null);
  
    // Setup inference batching if enabled
    setupDefaultInferenceBatching(serving);
  
    Serving newServing = servingFacade.merge(serving);
    servingWrapper.setServing(newServing);
    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(newServing));
  }
  
  private void updateServing(Project project, Users user, ServingWrapper servingWrapper)
    throws ServingException, ProjectException, KafkaException, UserException, ExecutionException, InterruptedException {
    Serving serving = servingWrapper.getServing();
    
    // Update serving request
    Serving oldServing = servingFacade.acquireLock(project, serving.getId());
    KubeToolServingController toolServingController = getServingController(oldServing);
  
    // If artifact already exists, check the asset scripts are not modified
    if (Objects.equals(oldServing.getModelName(), serving.getModelName()) && oldServing.getArtifactVersion() > 0 &&
      Objects.equals(oldServing.getArtifactVersion(), serving.getArtifactVersion())) {
      if (oldServing.getPredictor() != null && !oldServing.getPredictor().equals(serving.getPredictor())) {
        throw new IllegalArgumentException("Predictor script cannot change in an existent artifact");
      }
      if (oldServing.getTransformer() != null && !oldServing.getTransformer().equals(serving.getTransformer())) {
        throw new IllegalArgumentException("Transformer script cannot change in an existent artifact");
      }
    }
  
    // Set missing fields to keep consistency with the stored serving entity
    serving = servingFacade.fill(serving, oldServing);
  
    // Setup the Kafka topic for logging
    kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, oldServing);
  
    // Setup inference batching if enabled
    setupDefaultInferenceBatching(serving);
  
    try {
      // Avoid updating if there are no changes
      if (oldServing.equals(serving)) {
        return;
      }
    
      // Get current serving status
      KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, oldServing);
      ServingStatusEnum status = internalStatus.getServingStatus();
    
      // Merge serving fields
      Serving newServing = servingFacade.mergeServings(oldServing, serving);
      if (newServing.getDeployed() != null) {
        // If the serving is deployed, check the serving status
        if (!internalStatus.getAvailable() ||
            status == ServingStatusEnum.STARTING || status == ServingStatusEnum.UPDATING) {
          // If the serving is starting/updating or we can't get the nº of available replicas (inference
          // service not created yet), don't allow updates.
          throw new ServingException(RESTCodes.ServingErrorCode.UPDATE_ERROR, Level.FINE,
            "Deployment is starting. Please, try later.");
        }
        if (status == ServingStatusEnum.RUNNING || status == ServingStatusEnum.IDLE ||
            status == ServingStatusEnum.FAILED) {
          // If running, idle or failed. Update the inference service.
          String newRevision = servingUtils.getNewRevisionID(); // Generate pseudo-random revision id
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
        }
      }
      // Update the serving object in the database and serving wrapper
      serving = servingFacade.updateDbObject(newServing, project);
      servingWrapper.setServing(serving);
      servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));
    } finally {
      servingFacade.releaseLock(project, serving.getId());
    }
  }
  
  private ServingWrapper getServingInternal(Project project, Serving serving)
    throws ServingException {
    ServingWrapper servingWrapper = new ServingWrapper(serving);
    
    KubeToolServingController toolServingController = getServingController(serving);
    KubeServingInternalStatus internalStatus = toolServingController.getInternalStatus(project, serving);
    
    servingWrapper.setStatus(internalStatus.getServingStatus());
    servingWrapper.setAvailableReplicas(internalStatus.getAvailableReplicas());
    servingWrapper.setAvailableTransformerReplicas(internalStatus.getAvailableTransformerReplicas());
    servingWrapper.setCondition(internalStatus.getCondition());
  
    servingWrapper.setHopsworksInferencePath(internalStatus.getHopsworksInferencePath());
    servingWrapper.setModelServerInferencePath(internalStatus.getModelServerInferencePath());
    
    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));
    
    return servingWrapper;
  }
  
  private KubeToolServingController getServingController(Serving serving) {
    return serving.getServingTool() == ServingTool.KSERVE
      ? kubeKServeController
      : kubeDeploymentServingController;
  }
  
  private void setupDefaultInferenceBatching(Serving serving) {
    if (serving.getServingTool() != ServingTool.KSERVE) {
      return;
    }
    BatchingConfiguration batchingConfiguration = serving.getBatchingConfiguration();
    if (batchingConfiguration == null || !batchingConfiguration.isBatchingEnabled()) {
      return; // nothing to do
    }
    // set empty config with default values
    if (batchingConfiguration.getMaxBatchSize() == null) {
      batchingConfiguration.setMaxBatchSize(KubeServingUtils.INFERENCE_BATCHER_MAX_BATCH_SIZE);
    }
    if (batchingConfiguration.getMaxLatency() == null) {
      batchingConfiguration.setMaxLatency(KubeServingUtils.INFERENCE_BATCHER_MAX_LATENCY);
    }
    if (batchingConfiguration.getTimeout() == null) {
      batchingConfiguration.setTimeout(KubeServingUtils.INFERENCE_BATCHER_TIMEOUT);
    }
  }
}
