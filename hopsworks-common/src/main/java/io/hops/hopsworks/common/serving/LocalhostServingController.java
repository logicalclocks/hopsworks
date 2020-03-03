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

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.persistence.entity.serving.ServingType;
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

  public static final Integer PID_STOPPED = -2;
  public static final String SERVING_DIRS = "/serving/";

  @EJB
  private ServingFacade servingFacade;
  @EJB
  private KafkaServingHelper kafkaServingHelper;
  @EJB
  private LocalhostSkLearnServingController skLearnServingController;
  @EJB
  private LocalhostTfServingController tfServingController;
  
  /**
   * Gets a list of available servings for a project
   *
   * @param project the project to get servings for
   * @return a list of ServingWrapper DTOs with metadata of the servings
   */
  @Override
  public List<ServingWrapper> getServings(Project project) {
    List<Serving> servingList = servingFacade.findForProject(project);

    List<ServingWrapper> servingWrapperList = new ArrayList<>();
    for (Serving serving : servingList) {
      servingWrapperList.add(getServingInternal(serving));
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
  public ServingWrapper getServing(Project project, Integer id) {
    Serving serving = servingFacade.findByProjectAndId(project, id);
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
  public void deleteServings(Project project) throws ServingException {
    List<Serving> servingList = servingFacade.findForProject(project);
    for (Serving serving : servingList) {
      // Acquire lock
      servingFacade.acquireLock(project, serving.getId());

      ServingStatusEnum status = getServingStatus(serving);

      // getServingStatus returns STARTING if the PID is set to -2 and there is a lock.
      // If we reached this point, we just acquired a lock
      if (!status.equals(ServingStatusEnum.STARTING)) {
        killServingInstance(project, serving, false);
      }
      servingFacade.delete(serving);
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
  public void deleteServing(Project project, Integer id) throws ServingException {
    Serving serving = servingFacade.acquireLock(project, id);
    ServingStatusEnum status = getServingStatus(serving);

    // getServingStatus returns STARTING if the PID is set to -2 and there is a lock.
    // If we reached this point, we just acquired a lock
    if (!status.equals(ServingStatusEnum.STARTING)) {
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
      case STOPPED:
      case STARTING:
      case UPDATING:
        servingWrapper.setAvailableReplicas(0);
        break;
      case RUNNING:
        servingWrapper.setAvailableReplicas(1);
        servingWrapper.setNodePort(serving.getLocalPort());

    }

    servingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(serving));

    return servingWrapper;
  }
  
  /**
   * Starts or stop a serving instance (depending on the user command). Will call the controller for the corresponding
   * servingtype, such as Tensorflow or SkLearn
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

    Serving serving = servingFacade.acquireLock(project, servingId);
    ServingStatusEnum currentStatus = getServingStatus(serving);

    // getServingStatus returns STARTING if the PID is set to -2 and there is a lock.
    // If we reached this point, we just acquired a lock
    if (currentStatus == ServingStatusEnum.STARTING
        && command == ServingCommands.START) {
      startServingInstance(project, user, serving);

      // getServingStatus returns UPDATING if the PID is different than -2 and there is a lock.
      // If we reached this point, we just acquired a lock
    } else if (currentStatus == ServingStatusEnum.UPDATING &&
        command == ServingCommands.STOP) {
      killServingInstance(project, serving, true);
    } else {
      // Release lock before throwing the exception
      servingFacade.releaseLock(project, servingId);

      String userMsg = "Instance is already " + (command == ServingCommands.START ? "started" : "stopped");
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.FINE, userMsg);
    }
  }
  
  /**
   * Creates or updates a serving instance in the database.
   *
   * @param project the project of the serving instance
   * @param user the user making the request
   * @param newServing the serving to create or update
   * @throws ProjectException
   * @throws ServingException
   * @throws KafkaException
   * @throws ServiceException
   * @throws UserException
   */
  @Override
  public void createOrUpdate(Project project, Users user, ServingWrapper newServing)
      throws ProjectException, ServingException, KafkaException, UserException,
    InterruptedException, ExecutionException {
    Serving serving = newServing.getServing();
    if (serving.getId() == null) {
      // Create request
      serving.setCreated(new Date());
      serving.setCreator(user);
      serving.setProject(project);

      UUID uuid = UUID.randomUUID();
      serving.setLocalDir(uuid.toString());
      serving.setLocalPid(PID_STOPPED);
      serving.setInstances(1);

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newServing, serving, null);

      servingFacade.merge(serving);
    } else {
      Serving oldDbServing = servingFacade.acquireLock(project, serving.getId());
      // Get the status of the current instance
      ServingStatusEnum status = getServingStatus(oldDbServing);
      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newServing, serving, oldDbServing);
      // Update the object in the database
      Serving dbServing = servingFacade.updateDbObject(serving, project);
      if (status == ServingStatusEnum.RUNNING || status == ServingStatusEnum.UPDATING) {
        if (!oldDbServing.getName().equals(dbServing.getName()) ||
            !oldDbServing.getArtifactPath().equals(dbServing.getArtifactPath()) ||
            oldDbServing.isBatchingEnabled() != dbServing.isBatchingEnabled() ||
            oldDbServing.getVersion() > dbServing.getVersion()) {
          // To update the name and/or the artifact path we need to restart the server and/or the version as been
          // reduced. We need to restart the server
          restartServingInstance(project, user, oldDbServing, dbServing);
        } else {
          // To update the version call the script and download the new version in the directory
          // the server polls for new versions and it will pick it up.
          if(serving.getServingType() == ServingType.TENSORFLOW){
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
    }
  }
  
  /**
   * Gets the max number of serving instances, in the localhost serving version there will only be one instance
   *
   * @return 1
   */
  @Override
  public int getMaxNumInstances() {
    return 1;
  }
  
  
  private void startServingInstance(Project project, Users user, Serving serving) throws ServingException {
    if(serving.getServingType() == ServingType.TENSORFLOW){
      tfServingController.startServingInstance(project, user, serving);
    }
    if(serving.getServingType() == ServingType.SKLEARN){
      skLearnServingController.startServingInstance(project, user, serving);
    }
  }

  private void killServingInstance(Project project, Serving serving, boolean releaseLock)
      throws ServingException {
    if(serving.getServingType() == ServingType.TENSORFLOW){
      tfServingController.killServingInstance(project, serving, releaseLock);
    }
    if(serving.getServingType() == ServingType.SKLEARN){
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

  private ServingStatusEnum getServingStatus(Serving serving) {
    // Compute status
    if (serving.getLocalPid().equals(PID_STOPPED) && serving.getLockIP() == null) {
      // The Pid is not in the database, and nobody has the lock, the instance is stopped
      return ServingStatusEnum.STOPPED;
    } else if (serving.getLocalPid().equals(PID_STOPPED)) {
      // The Pid is -1, but someone has the lock, the instance is starting
      return ServingStatusEnum.STARTING;
    } else if (!serving.getLocalPid().equals(PID_STOPPED) && serving.getLockIP() == null){
      // The Pid is in the database and nobody as the lock. Instance is running
      return ServingStatusEnum.RUNNING;
    } else {
      // Someone is updating the instance.
      return ServingStatusEnum.UPDATING;
    }
  }
}
