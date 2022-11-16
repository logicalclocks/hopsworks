/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.serving;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.serving.inference.LocalhostSkLearnInferenceUtils;
import io.hops.hopsworks.common.serving.inference.LocalhostTfInferenceUtils;
import io.hops.hopsworks.common.serving.util.ServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.common.serving.sklearn.LocalhostSkLearnServingController;
import io.hops.hopsworks.common.serving.tf.LocalhostTfServingController;
import io.hops.hopsworks.common.serving.util.KafkaServingHelper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Contains the common functionality between localhost serving controllers, the specific functionality is provided by
 * serving-type-specific controllers, e.g SkLearnServingController, tfServingController.
 */
@LocalhostStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalhostServingController implements ServingController {

  public static final String CID_FAILED = "failed";
  public static final String CID_STOPPED = "stopped";
  public static final String SERVING_DIRS = "/serving/";

  @EJB
  private ServingFacade servingFacade;
  @EJB
  private KafkaServingHelper kafkaServingHelper;
  @EJB
  private LocalhostSkLearnServingController skLearnServingController;
  @EJB
  private LocalhostTfServingController tfServingController;
  @EJB
  private ServingUtils servingUtils;
  @EJB
  private LocalhostTfInferenceUtils localhostTfInferenceUtils;
  @EJB
  private LocalhostSkLearnInferenceUtils localhostSkLearnInferenceUtils;
  
  /**
   * Gets a list of available servings for a project
   *
   * @param project the project to get servings for
   * @return a list of ServingWrapper DTOs with metadata of the servings
   */
  @Override
  public List<ServingWrapper> getAll(Project project, String modelNameFilter, Integer modelVersionFilter,
    ServingStatusEnum statusFilter)
      throws ServingException {
    List<Serving> servingList;
    if(Strings.isNullOrEmpty(modelNameFilter)) {
      servingList = servingFacade.findForProject(project);
    } else if (modelVersionFilter == null) {
      servingList = servingFacade.findForProjectAndModel(project, modelNameFilter);
    } else {
      servingList = servingFacade.findForProjectAndModelVersion(project, modelNameFilter, modelVersionFilter);
    }

    List<ServingWrapper> servingWrapperList = new ArrayList<>();
    for (Serving serving : servingList) {
      ServingWrapper servingWrapper = getServingInternal(serving);
      // If status filter is set only add servings with the defined status
      if(statusFilter != null && servingWrapper.getStatus() != statusFilter) {
        continue;
      }
      servingWrapperList.add(servingWrapper);
    }

    return servingWrapperList;
  }
  
  /**
   * Gets an individual serving with a specific id from the database
   *
   * @param project the project where the serving resides
   * @param id the id of the serving to get
   * @return a ServingWrapper with metadata of the serving
   */
  @Override
  public ServingWrapper get(Project project, Integer id) throws ServingException {
    Serving serving = servingFacade.findByProjectAndId(project, id);
    if (serving == null) {
      return null;
    }

    return getServingInternal(serving);
  }
  
  /**
   * Gets an individual serving with a specific name from the database
   *
   * @param project the project where the serving resides
   * @param name the name of the serving to get
   * @return a ServingWrapper with metadata of the serving
   */
  @Override
  public ServingWrapper get(Project project, String name) throws ServingException {
    Serving serving = servingFacade.findByProjectAndName(project, name);
    if (serving == null) {
      return null;
    }
    
    return getServingInternal(serving);
  }
  
  /**
   * Deletes all servings in a project (used for project cleanup)
   *
   * @param project the project to delete all servings for
   * @throws ServingException thrown if a lock for getting the serving could not be acquired
   */
  @Override
  public void deleteAll(Project project) throws ServingException {
    for (Serving serving : servingFacade.findForProject(project)) {
      delete(project, serving.getId());
    }
  }
  
  /**
   * Deletes a specific serving from a project
   *
   * @param project the project that contains the serving
   * @param id the id of the serving
   * @throws ServingException if the lock could not be acquired
   */
  @Override
  public void delete(Project project, Integer id) throws ServingException {
    // acquire lock
    Serving serving = servingFacade.acquireLock(project, id);
  
    // get status
    ServingStatusEnum status = getServingStatus(serving);
  
    // if we reached this point, we just acquired a lock
    if (!status.equals(ServingStatusEnum.STARTING)) {
      // if cid is not stopped
      killServingInstance(project, serving, false);
    }
    servingFacade.delete(serving);
  }

  @Override
  public String getClassName() {
    return LocalhostServingController.class.getName();
  }
  
  /**
   * Gets the internal representation of a serving. The internal represenation contains extra information that
   * is not exposed to the user, such as status, available replicas, nodeport, and extended kafka details
   *
   * @param serving the serving to get the internal representation for
   * @return internal representation of the serving
   */
  private ServingWrapper getServingInternal(Serving serving) {
    ServingWrapper servingWrapper = new ServingWrapper(serving);

    ServingStatusEnum status = getServingStatus(serving);
    servingWrapper.setStatus(status);
    switch (status) {
      case CREATED:
      case STOPPED:
        servingWrapper.setCondition(ServingStatusCondition.getStoppedSuccessCondition());
        break;
      case STOPPING:
        servingWrapper.setCondition(ServingStatusCondition.getStoppedInProgressCondition());
        break;
      case FAILED:
        servingWrapper.setCondition(ServingStatusCondition.getStartedFailedCondition("deployment terminated " +
          "unsuccessfully"));
        break;
      case UPDATING:
      case STARTING:
        servingWrapper.setCondition(ServingStatusCondition.getStartedInProgressCondition());
        break;
      case IDLE:
      case RUNNING:
        servingWrapper.setCondition(ServingStatusCondition.getReadySuccessCondition());
        break;
    }
    
    if (status == ServingStatusEnum.RUNNING) {
      servingWrapper.setAvailableReplicas(1);
      servingWrapper.setInternalPort(serving.getLocalPort());
    } else {
      servingWrapper.setAvailableReplicas(0);
      if (status == ServingStatusEnum.IDLE) {
        servingWrapper.setInternalPort(serving.getLocalPort());
      } else {
        servingWrapper.setInternalPort(null);
      }
    }
    
    String path;
    if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      path = localhostTfInferenceUtils.getPath(serving.getName(), serving.getModelVersion(), null);
    } else if (serving.getModelServer() == ModelServer.PYTHON) {
      path = localhostSkLearnInferenceUtils.getPath(null);
    } else {
      throw new UnsupportedOperationException("Model server not supported as local serving");
    }
    servingWrapper.setModelServerInferencePath(path);
    servingWrapper.setHopsworksInferencePath("/project/" + serving.getProject().getId() + "/inference/models/" +
      serving.getName());
    
    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));
    return servingWrapper;
  }
  
  /**
   * Starts or stop a serving instance (depending on the user command). Will call the controller for the corresponding
   * model server, such as Tensorflow Serving or Python
   *
   * @param project the project where the serving resides
   * @param user the user making the request
   * @param servingId the id of the serving
   * @param command the command (start or stop)
   * @throws ServingException if the serving could not be started or lock could be acquired
   */
  @Override
  public void startOrStop(Project project, Users user, Integer servingId, ServingCommands command)
      throws ServingException {
    
    // acquire lock
    Serving serving = servingFacade.acquireLock(project, servingId);
    
    // get status
    ServingStatusEnum status = getServingStatus(serving);

    if (command == ServingCommands.START && status == ServingStatusEnum.STARTING) {
      // if status is Starting, cid is stopped and the lock is acquired
      startServingInstance(project, user, serving);
    } else if (command == ServingCommands.STOP &&
      (status == ServingStatusEnum.UPDATING || status == ServingStatusEnum.FAILED)) {
      // if status is updating, cid is neither stopped nor failed, and the lock is acquired
      killServingInstance(project, serving, true);
    } else {
      // release lock before throwing the exception
      servingFacade.releaseLock(project, servingId);

      String userMsg = "Instance is already " + (command == ServingCommands.START ?
        ServingStatusEnum.STARTING.toString() : ServingStatusEnum.STOPPED.toString()).toLowerCase();
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.FINE, userMsg);
    }
  }
  
  /**
   * Creates or updates a serving instance in the database.
   *
   * @param project the project of the serving instance
   * @param user the user making the request
   * @param servingWrapper the serving to create or update
   * @throws ProjectException
   * @throws ServingException
   * @throws KafkaException
   * @throws ServiceException
   * @throws UserException
   */
  @Override
  public void put(Project project, Users user, ServingWrapper servingWrapper)
      throws ProjectException, ServingException, KafkaException, UserException,
    InterruptedException, ExecutionException {
    
    Serving serving = servingWrapper.getServing();
    
    if (serving.getId() == null) {
      // Create request
      serving.setCreated(new Date());
      serving.setCreator(user);
      serving.setProject(project);

      UUID uuid = UUID.randomUUID();
      serving.setLocalDir(uuid.toString());
      serving.setCid(CID_STOPPED);
      serving.setInstances(1);

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, null);
      
      Serving newServing = servingFacade.merge(serving);
      servingWrapper.setServing(newServing);
      servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(newServing));
    } else {
      Serving oldDbServing = servingFacade.acquireLock(project, serving.getId());
      // Get the status of the current instance
      ServingStatusEnum status = getServingStatus(oldDbServing);
      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, servingWrapper, serving, oldDbServing);
      // Update the object in the database
      Serving dbServing = servingFacade.updateDbObject(serving, project);
      if (status == ServingStatusEnum.RUNNING || status == ServingStatusEnum.UPDATING ||
          status == ServingStatusEnum.IDLE) {
        String newRevision = servingUtils.getNewRevisionID();
        serving.setRevision(newRevision);
        Boolean samePredictor = (oldDbServing.getPredictor() == null && dbServing.getPredictor() == null) ||
          (oldDbServing.getPredictor() != null && dbServing.getPredictor() != null &&
            oldDbServing.getPredictor().equals(dbServing.getPredictor()));
        if (!oldDbServing.getName().equals(dbServing.getName()) ||
            !oldDbServing.getModelPath().equals(dbServing.getModelPath()) ||
            !samePredictor ||
            oldDbServing.getBatchingConfiguration() != dbServing.getBatchingConfiguration() ||
            oldDbServing.getModelVersion() > dbServing.getModelVersion()) {
          // To update the name and/or the artifact path we need to restart the server and/or the version as been
          // reduced. We need to restart the server
          restartServingInstance(project, user, oldDbServing, dbServing);
        } else {
          // To update the version call the script and download the new version in the directory
          // the server polls for new versions and it will pick it up.
          if(serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
            tfServingController.updateModelVersion(project, user, dbServing);
          } else {
            //If we do not need to update model version there is nothing left to do and we can release the lock
            servingFacade.releaseLock(project, serving.getId());
          }
        }
      } else {
        // The instance is not running, nothing else to do. Just release the lock.
        servingFacade.releaseLock(project, serving.getId());
      }
      servingWrapper.setServing(dbServing);
      servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(dbServing));
    }
  }
  
  @Override
  public List<ServingLogs> getLogs(Project project, Integer servingId, String component, Integer tailingLines)
    throws ServingException {
    throw new ServingException(RESTCodes.ServingErrorCode.KUBERNETES_NOT_INSTALLED, Level.FINE, "Direct access to " +
      "server logs is only supported in Kubernetes deployments");
  }
  
  private void startServingInstance(Project project, Users user, Serving serving) throws ServingException {
    String newRevision = servingUtils.getNewRevisionID();
    serving.setRevision(newRevision);
    
    if(serving.getModelServer() == ModelServer.TENSORFLOW_SERVING){
      tfServingController.startServingInstance(project, user, serving);
    } else if(serving.getModelServer() == ModelServer.PYTHON){
      skLearnServingController.startServingInstance(project, user, serving);
    }
  }

  private void killServingInstance(Project project, Serving serving, boolean releaseLock)
      throws ServingException {
    if(serving.getModelServer() == ModelServer.TENSORFLOW_SERVING){
      tfServingController.killServingInstance(project, serving, releaseLock);
    } else if(serving.getModelServer() == ModelServer.PYTHON){
      skLearnServingController.killServingInstance(project, serving, releaseLock);
    }
  }

  private void restartServingInstance(Project project, Users user, Serving currentInstance,
                                     Serving newInstance) throws ServingException {
    // Kill current Serving instance
    killServingInstance(project, currentInstance, false);

    // Start new Serving instance
    startServingInstance(project, user, newInstance);
  }

  public static ServingStatusEnum getServingStatus(Serving serving) {
    if (serving.getCid().equals(CID_STOPPED)) {
      // if cid is stopped,
      if (serving.getLockIP() != null) {
        // and the lock is acquired, the instance is starting
        return ServingStatusEnum.STARTING;
      }
      // otherwise the instance is stopped or just created
      return serving.getRevision() == null ? ServingStatusEnum.CREATED : ServingStatusEnum.STOPPED;
    }
    if (serving.getCid().equals(CID_FAILED)) {
      // if cid is failed, the instance failed to start or during runtime
      return ServingStatusEnum.FAILED;
    }
    // otherwise, instance is updating or running
    return serving.getLockIP() != null
      ? ServingStatusEnum.UPDATING
      : ServingStatusEnum.RUNNING;
  }
}
