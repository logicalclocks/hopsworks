/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.tf;

import com.google.common.io.Files;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.serving.TfServingFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.KafkaException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.serving.KafkaServingHelper;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.hdfs.HdfsUsersController.USER_NAME_DELIMITER;

@Alternative
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalhostTfServingController implements TfServingController {

  private final static Logger logger = Logger.getLogger(LocalhostTfServingController.class.getName());

  public static final String SERVING_DIRS = "/serving/";
  public static final Integer PID_STOPPED = -2;

  @EJB
  private TfServingFacade tfServingFacade;
  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private KafkaServingHelper kafkaServingHelper;

  @Override
  public List<TfServingWrapper> getTfServings(Project project) throws TfServingException {
    List<TfServing> tfServingList = tfServingFacade.findForProject(project);

    List<TfServingWrapper> tfServingWrapperList = new ArrayList<>();
    for (TfServing tfServing : tfServingList) {
      tfServingWrapperList.add(getTfServingInternal(tfServing));
    }

    return tfServingWrapperList;
  }

  @Override
  public TfServingWrapper getTfServing(Project project, Integer id) throws TfServingException {
    TfServing tfServing = tfServingFacade.findByProjectAndId(project, id);
    if (tfServing == null) {
      return null;
    }

    return getTfServingInternal(tfServing);
  }

  @Override
  public void deleteTfServings(Project project) throws TfServingException {
    List<TfServing> tfServingList = tfServingFacade.findForProject(project);
    for (TfServing tfServing : tfServingList) {
      // Acquire lock
      tfServingFacade.acquireLock(project, tfServing.getId());

      TfServingStatusEnum status = getTfServingStatus(tfServing);

      // getTfServingStatus returns STARTING if the PID is set to -2 and there is a lock.
      // If we reached this point, we just acquired a lock
      if (!status.equals(TfServingStatusEnum.STARTING)) {
        killTfServingInstance(project, tfServing, false);
      }
      tfServingFacade.delete(tfServing);
    }
  }

  @Override
  public void deleteTfServing(Project project, Integer id) throws TfServingException {
    TfServing tfServing = tfServingFacade.acquireLock(project, id);
    TfServingStatusEnum status = getTfServingStatus(tfServing);

    // getTfServingStatus returns STARTING if the PID is set to -2 and there is a lock.
    // If we reached this point, we just acquired a lock
    if (!status.equals(TfServingStatusEnum.STARTING)) {
      killTfServingInstance(project, tfServing, false);
    }
    tfServingFacade.delete(tfServing);
  }

  @Override
  public void checkDuplicates(Project project, TfServingWrapper tfServingWrapper) throws TfServingException {
    TfServing serving = tfServingFacade.findByProjectModelName(project,
        tfServingWrapper.getTfServing().getModelName());
    if (serving != null && !serving.getId().equals(tfServingWrapper.getTfServing().getId())) {
      // There is already an entry for this project
      throw new TfServingException(RESTCodes.TfServingErrorCode.DUPLICATEDENTRY, Level.FINE);
    }
  }

  @Override
  public void createOrUpdate(Project project, Users user, TfServingWrapper newTfServingWrapper)
      throws KafkaException, UserException, ProjectException, ServiceException, TfServingException {

    TfServing serving = newTfServingWrapper.getTfServing();
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
      kafkaServingHelper.setupKafkaServingTopic(project, newTfServingWrapper, serving, null);

      tfServingFacade.merge(serving);
    } else {
      TfServing oldDbTfServing = tfServingFacade.acquireLock(project, serving.getId());

      // Get the status of the current instance
      TfServingStatusEnum status = getTfServingStatus(oldDbTfServing);

      // Setup the Kafka topic for logging
      kafkaServingHelper.setupKafkaServingTopic(project, newTfServingWrapper, serving, oldDbTfServing);

      // Update the object in the database
      TfServing dbTfServing = tfServingFacade.updateDbObject(serving, project);

      if (status == TfServingStatusEnum.RUNNING || status == TfServingStatusEnum.UPDATING) {
        if (!oldDbTfServing.getModelName().equals(dbTfServing.getModelName()) ||
            !oldDbTfServing.getModelPath().equals(dbTfServing.getModelPath()) ||
            oldDbTfServing.isBatchingEnabled() != dbTfServing.isBatchingEnabled() ||
            oldDbTfServing.getVersion() > dbTfServing.getVersion()) {
          // To update the name and/or the model path we need to restart the server and/or the version as been
          // reduced. We need to restart the server
          restartTfServingInstance(project, user, oldDbTfServing, dbTfServing);
        } else {
          // To update the version call the script and download the new version in the directory
          // the server polls for new versions and it will pick it up.
          updateModelVersion(project, user, dbTfServing);
        }
      } else {
        // The instance is not running, nothing else to do. Just release the lock.
        tfServingFacade.releaseLock(project, serving.getId());
      }
    }
  }

  @Override
  public void startOrStop(Project project, Users user, Integer tfServingId, TfServingCommands command)
      throws TfServingException {

    TfServing tfServing = tfServingFacade.acquireLock(project, tfServingId);

    TfServingStatusEnum currentStatus = getTfServingStatus(tfServing);

    // getTfServingStatus returns STARTING if the PID is set to -2 and there is a lock.
    // If we reached this point, we just acquired a lock
    if (currentStatus == TfServingStatusEnum.STARTING
        && command == TfServingCommands.START) {
      startTfServingInstance(project, user, tfServing);

      // getTfServingStatus returns UPDATING if the PID is different than -2 and there is a lock.
      // If we reached this point, we just acquired a lock
    } else if (currentStatus == TfServingStatusEnum.UPDATING &&
        command == TfServingCommands.STOP) {
      killTfServingInstance(project, tfServing, true);
    } else {
      // Release lock before throwing the exception
      tfServingFacade.releaseLock(project, tfServingId);

      String userMsg = "Instance is already " + (command == TfServingCommands.START ? "started" : "stopped");
      throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERROR, Level.FINE, userMsg);
    }
  }

  @Override
  public int getMaxNumInstances() {
    return 1;
  }

  @Override
  public String getClassName() {
    return LocalhostTfServingController.class.getName();
  }

  private TfServingWrapper getTfServingInternal(TfServing tfServing) {
    TfServingWrapper tfServingWrapper = new TfServingWrapper(tfServing);

    TfServingStatusEnum status = getTfServingStatus(tfServing);
    tfServingWrapper.setStatus(status);
    switch (status) {
      case STOPPED:
      case STARTING:
      case UPDATING:
        tfServingWrapper.setAvailableReplicas(0);
        break;
      case RUNNING:
        tfServingWrapper.setAvailableReplicas(1);
        tfServingWrapper.setNodePort(tfServing.getLocalPort());

    }

    tfServingWrapper.setKafkaTopicDTO(kafkaServingHelper.buildTopicDTO(tfServing));

    return tfServingWrapper;
  }

  private TfServingStatusEnum getTfServingStatus(TfServing tfServing) {
    // Compute status
    if (tfServing.getLocalPid().equals(PID_STOPPED) && tfServing.getLockIP() == null) {
      // The Pid is not in the database, and nobody has the lock, the instance is stopped
      return TfServingStatusEnum.STOPPED;
    } else if (tfServing.getLocalPid().equals(PID_STOPPED)) {
      // The Pid is -1, but someone has the lock, the instance is starting
      return TfServingStatusEnum.STARTING;
    } else if (!tfServing.getLocalPid().equals(PID_STOPPED) && tfServing.getLockIP() == null){
      // The Pid is in the database and nobody as the lock. Instance is running
      return TfServingStatusEnum.RUNNING;
    } else {
      // Someone is updating the instance.
      return TfServingStatusEnum.UPDATING;
    }
  }

  private void updateModelVersion(Project project, Users user, TfServing tfServing) throws TfServingException {
    // TFServing polls for new version of the model in the directory
    // if a new version is downloaded it starts serving it
    String script = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";

    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS, tfServing.getLocalDir());

    String[] command = {"/usr/bin/sudo", script, "update",
        tfServing.getModelName(),
        Paths.get(tfServing.getModelPath(), tfServing.getVersion().toString()).toString(),
        secretDir.toString(),
        project.getName() + USER_NAME_DELIMITER + user.getUsername()};

    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);

    // Materialized TLS certificates to be able to read the model
    if (settings.getHopsRpcTls()) {
      try {
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (IOException e) {
        throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null,
          e.getMessage(), e);
      } finally {
        tfServingFacade.releaseLock(project, tfServing.getId());
      }
    }

    try {
      Process process = pb.start();
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
      throw new TfServingException(RESTCodes.TfServingErrorCode.UPDATEERROR, Level.SEVERE,
        "tfServing id: " + tfServing.getId(), ex.getMessage(), ex);
    } finally {
      if (settings.getHopsRpcTls()) {
        certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      }

      tfServingFacade.releaseLock(project, tfServing.getId());
    }
  }

  private void killTfServingInstance(Project project, TfServing tfServing, boolean releaseLock)
      throws TfServingException {

    String script = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";

    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + tfServing.getLocalDir());
    String[] command = {"/usr/bin/sudo", script, "kill", String.valueOf(tfServing.getLocalPid()),
        String.valueOf(tfServing.getLocalPort()), secretDir.toString()};

    logger.log(Level.INFO, Arrays.toString(command));
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
      Process process = pb.start();
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
      throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERROR, Level.SEVERE,
        "tfServing id: " + tfServing.getId(), ex.getMessage(), ex);
    }

    tfServing.setLocalPid(PID_STOPPED);
    tfServing.setLocalPort(-1);
    tfServingFacade.updateDbObject(tfServing, project);

    if (releaseLock) {
      // During the restart the lock is needed until the tfServing instance is actually restarted.
      // The startTfServingInstance method is responsible of releasing the lock on the db entry
      // During the termination phase, this method is responsible of releasing the lock
      // In case of termination + deletion, we don't release the lock as the entry will be removed from the db.
      tfServingFacade.releaseLock(project, tfServing.getId());
    }
  }

  private void startTfServingInstance(Project project, Users user, TfServing tfServing) throws TfServingException{

    String script = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";

    // TODO(Fabio) this is bad as we don't know if the port is used or not
    Integer grpcPort = ThreadLocalRandom.current().nextInt(40000, 59999);
    Integer restPort = ThreadLocalRandom.current().nextInt(40000, 59999);

    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + tfServing.getLocalDir());

    String[] shCommnad = new String[]{"/usr/bin/sudo", script, "start",
        tfServing.getModelName(),
        Paths.get(tfServing.getModelPath(), tfServing.getVersion().toString()).toString(),
        String.valueOf(grpcPort),
        String.valueOf(restPort),
        secretDir.toString(),
        project.getName() + USER_NAME_DELIMITER + user.getUsername(),
        tfServing.isBatchingEnabled() ? "1" : "0",
        project.getName().toLowerCase()};

    logger.log(Level.INFO, Arrays.toString(shCommnad));

    // Materialized TLS certificates to be able to read the model
    if (settings.getHopsRpcTls()) {
      try {
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (IOException e) {
        throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null,e.getMessage(),
          e);
      } finally {
        // Release lock on the tfServing entry
        tfServingFacade.releaseLock(project, tfServing.getId());
      }
    }

    ProcessBuilder pb = new ProcessBuilder(shCommnad);
    Process process = null;
    try {
      // Send both stdout and stderr to the same stream
      pb.redirectErrorStream(true);
      process = pb.start();

      // Wait until the launcher bash script has finished
      process.waitFor();

      if (process.exitValue() != 0) {
        // Startup process failed for some reason
        tfServing.setLocalPid(PID_STOPPED);
        tfServingFacade.updateDbObject(tfServing, project);
        throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERRORINT, Level.INFO);
      }

      // Read the pid for TensorFlow Serving server
      Path pidFilePath = Paths.get(secretDir.toString(), "tfserving.pid");
      String pidContents = Files.readFirstLine(pidFilePath.toFile(), Charset.defaultCharset());

      // Update the info in the db
      tfServing.setLocalPid(Integer.valueOf(pidContents));
      tfServing.setLocalPort(restPort);
      tfServingFacade.updateDbObject(tfServing, project);
    } catch (Exception ex) {
      if (process != null) {
        process.destroyForcibly();
      }

      // Startup process failed for some reason
      tfServing.setLocalPid(PID_STOPPED);
      tfServingFacade.updateDbObject(tfServing, project);

      throw new TfServingException(RESTCodes.TfServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null,
          ex.getMessage(), ex);

    } finally {
      if (settings.getHopsRpcTls()) {
        certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      }
      // release lock on the tfServing entry
      tfServingFacade.releaseLock(project, tfServing.getId());
    }
  }

  private void restartTfServingInstance(Project project, Users user, TfServing currentInstance,
                                        TfServing newInstance) throws TfServingException {
    // Kill current TfServing instance
    killTfServingInstance(project, currentInstance, false);

    // Start new TfServing instance
    startTfServingInstance(project, user, newInstance);
  }
}
