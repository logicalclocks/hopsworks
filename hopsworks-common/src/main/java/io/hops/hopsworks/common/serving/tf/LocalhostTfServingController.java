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
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.hdfs.HdfsUsersController.USER_NAME_DELIMITER;
import static io.hops.hopsworks.common.serving.LocalhostServingController.PID_STOPPED;
import static io.hops.hopsworks.common.serving.LocalhostServingController.SERVING_DIRS;

/**
 * Localhost Tensorflow Serving Controller
 *
 * Launches/Kills a local tensorflow-serving-server for serving tensorflow Models
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalhostTfServingController {

  private final static Logger logger = Logger.getLogger(LocalhostTfServingController.class.getName());

  @EJB
  private ServingFacade servingFacade;
  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private InodeFacade inodeFacade;
  
  public int getMaxNumInstances() {
    return 1;
  }
  
  public String getClassName() {
    return LocalhostTfServingController.class.getName();
  }
  
  /**
   * Updates the model version that is being served of an existing tfserving instance. The new model is copied to the
   * secret directory where the serving instance is running and then the server will automatically pick up the new
   * version.
   *
   * @param project the project of the serving instance
   * @param user the user making the request
   * @param serving the serving instance to update the model version for
   * @throws ServingException
   */
  public void updateModelVersion(Project project, Users user, Serving serving) throws ServingException {
    // TFServing polls for new version of the model in the directory
    // if a new version is downloaded it starts serving it
    String script = settings.getSudoersDir() + "/tfserving.sh";

    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS, serving.getLocalDir());
  
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(script)
        .addCommand("update")
        .addCommand(serving.getName())
        .addCommand(Paths.get(serving.getArtifactPath(), serving.getVersion().toString()).toString())
        .addCommand(secretDir.toString())
        .addCommand(project.getName() + USER_NAME_DELIMITER + user.getUsername())
        .ignoreOutErrStreams(true)
        .setWaitTimeout(2L, TimeUnit.MINUTES)
        .build();
    logger.log(Level.INFO, processDescriptor.toString());
    
    // Materialized TLS certificates to be able to read the model
    if (settings.getHopsRpcTls()) {
      try {
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (IOException e) {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null,
          e.getMessage(), e);
      } finally {
        servingFacade.releaseLock(project, serving.getId());
      }
    }

    try {
      osProcessExecutor.execute(processDescriptor);
    } catch (IOException ex) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATEERROR, Level.SEVERE,
        "serving id: " + serving.getId(), ex.getMessage(), ex);
    } finally {
      if (settings.getHopsRpcTls()) {
        certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      }

      servingFacade.releaseLock(project, serving.getId());
    }
  }
  
  /**
   * Stops a Tensorflow serving instance by killing the process with the corresponding PID
   *
   * @param project the project where the tensorflow serving instance is running
   * @param serving the serving instance to stop
   * @param releaseLock boolean flag deciding whether to release the lock afterwards.
   * @throws ServingException
   */
  public void killServingInstance(Project project, Serving serving, boolean releaseLock)
      throws ServingException {
    String script = settings.getSudoersDir() + "/tfserving.sh";
    
    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + serving.getLocalDir());

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(script)
        .addCommand("kill")
        .addCommand(String.valueOf(serving.getLocalPid()))
        .addCommand(String.valueOf(serving.getLocalPort()))
        .addCommand(secretDir.toString())
        .ignoreOutErrStreams(true)
        .build();

    logger.log(Level.INFO, processDescriptor.toString());
    try {
      osProcessExecutor.execute(processDescriptor);
    } catch (IOException ex) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERROR, Level.SEVERE,
        "serving id: " + serving.getId(), ex.getMessage(), ex);
    }

    serving.setLocalPid(PID_STOPPED);
    serving.setLocalPort(-1);
    servingFacade.updateDbObject(serving, project);

    if (releaseLock) {
      // During the restart the lock is needed until the serving instance is actually restarted.
      // The startTfServingInstance method is responsible of releasing the lock on the db entry
      // During the termination phase, this method is responsible of releasing the lock
      // In case of termination + deletion, we don't release the lock as the entry will be removed from the db.
      servingFacade.releaseLock(project, serving.getId());
    }
  }
  
  /**
   * Starts a Tensorflow serving instance. Executes the tfserving bash script to launch a tensorflow serving
   * server as serving-user and localize the tf-model from HDFS server. It records the PID of the server for monitoring.
   *
   * @param project the project to start the serving in
   * @param user the user starting the serving
   * @param serving the serving instance to start (tfserving servingtype)
   * @throws ServingException
   */
  public void startServingInstance(Project project, Users user, Serving serving) throws ServingException {
    String script = settings.getSudoersDir() + "/tfserving.sh";

    // TODO(Fabio) this is bad as we don't know if the port is used or not
    Integer grpcPort = ThreadLocalRandom.current().nextInt(40000, 59999);
    Integer restPort = ThreadLocalRandom.current().nextInt(40000, 59999);

    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + serving.getLocalDir());

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(script)
        .addCommand("start")
        .addCommand(serving.getName())
        .addCommand(Paths.get(serving.getArtifactPath(), serving.getVersion().toString()).toString())
        .addCommand(String.valueOf(grpcPort))
        .addCommand(String.valueOf(restPort))
        .addCommand(secretDir.toString())
        .addCommand(project.getName() + USER_NAME_DELIMITER + user.getUsername())
        .addCommand(serving.isBatchingEnabled() ? "1" : "0")
        .addCommand(project.getName().toLowerCase())
        .setWaitTimeout(2L, TimeUnit.MINUTES)
        .ignoreOutErrStreams(true)
        .build();
    logger.log(Level.INFO, processDescriptor.toString());

    // Materialized TLS certificates to be able to read the model
    if (settings.getHopsRpcTls()) {
      try {
        certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      } catch (IOException e) {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE,
            null, e.getMessage(), e);
      } finally {
        // Release lock on the serving entry
        servingFacade.releaseLock(project, serving.getId());
      }
    }

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);

      if (processResult.getExitCode() != 0) {
        // Startup process failed for some reason
        serving.setLocalPid(PID_STOPPED);
        servingFacade.updateDbObject(serving, project);
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO);
      }

      // Read the pid for TensorFlow Serving server
      Path pidFilePath = Paths.get(secretDir.toString(), "tfserving.pid");
      String pidContents = Files.readFirstLine(pidFilePath.toFile(), Charset.defaultCharset());

      // Update the info in the db
      serving.setLocalPid(Integer.valueOf(pidContents));
      serving.setLocalPort(restPort);
      servingFacade.updateDbObject(serving, project);
    } catch (Exception ex) {
      // Startup process failed for some reason
      serving.setLocalPid(PID_STOPPED);
      servingFacade.updateDbObject(serving, project);

      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE, null,
          ex.getMessage(), ex);

    } finally {
      if (settings.getHopsRpcTls()) {
        certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
      }
      // release lock on the serving entry
      servingFacade.releaseLock(project, serving.getId());
    }
  }
}
