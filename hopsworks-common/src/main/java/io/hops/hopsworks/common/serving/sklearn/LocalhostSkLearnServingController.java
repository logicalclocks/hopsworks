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

package io.hops.hopsworks.common.serving.sklearn;

import com.google.common.io.Files;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.Utils;
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
 * Localhost SkLearn Serving Controller
 *
 * Launches/Kills a local Flask Server for Serving SkLearn Models
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalhostSkLearnServingController {

  private final static Logger logger = Logger.getLogger(LocalhostSkLearnServingController.class.getName());

  @EJB
  private ServingFacade servingFacade;
  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  
  public int getMaxNumInstances() {
    return 1;
  }
  
  public String getClassName() {
    return LocalhostSkLearnServingController.class.getName();
  }
  
  /**
   * Stops a SKLearn serving instance by killing the process with the corresponding PID
   *
   * @param project the project where the sklearn instance is running
   * @param serving the serving instance to stop
   * @param releaseLock boolean flag deciding whether to release the lock afterwards.
   * @throws ServingException
   */
  public void killServingInstance(Project project, Serving serving, boolean releaseLock)
      throws ServingException {
    String script = settings.getSudoersDir() + "/sklearn_serving.sh";

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
      // The startSkLearnServingInstance method is responsible of releasing the lock on the db entry
      // During the termination phase, this method is responsible of releasing the lock
      // In case of termination + deletion, we don't release the lock as the entry will be removed from the db.
      servingFacade.releaseLock(project, serving.getId());
    }
  }
  
  /**
   * Starts a SkLearn serving instance. Executes the sklearn bash script to launch a Flask server as serving-user
   * in the project's anaconda environment. It records the PID of the server for monitoring.
   *
   * @param project the project to start the serving in
   * @param user the user starting the serving
   * @param serving the serving instance to start (sklearn servingtype)
   * @throws ServingException
   */
  public void startServingInstance(Project project, Users user, Serving serving) throws ServingException {
    String script = settings.getSudoersDir() + "/sklearn_serving.sh";
    Integer port = ThreadLocalRandom.current().nextInt(40000, 59999);
    Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + serving.getLocalDir());
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(script)
        .addCommand("start")
        .addCommand(Utils.getFileName(Paths.get(serving.getArtifactPath()).toString()))
        .addCommand(Paths.get(serving.getArtifactPath()).toString())
        .addCommand(String.valueOf(port))
        .addCommand(secretDir.toString())
        .addCommand(project.getName() + USER_NAME_DELIMITER + user.getUsername())
        .addCommand(project.getName().toLowerCase())
        .addCommand(settings.getAnacondaProjectDir(project) + "/bin/python")
        .addCommand(certificateMaterializer.getUserTransientKeystorePath(project, user))
        .addCommand(certificateMaterializer.getUserTransientTruststorePath(project, user))
        .addCommand(certificateMaterializer.getUserTransientPasswordPath(project, user))
        .addCommand(serving.getName())
        .setWaitTimeout(2L, TimeUnit.MINUTES)
        .ignoreOutErrStreams(true)
        .build();
    logger.log(Level.INFO, processDescriptor.toString());
    try {
      // Materialized TLS certificates so that user can read from HDFS inside python script
      certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        // Startup process failed for some reason
        serving.setLocalPid(PID_STOPPED);
        servingFacade.updateDbObject(serving, project);
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.INFO);
      }

      // Read the pid for SkLearn Serving Flask server
      Path pidFilePath = Paths.get(secretDir.toString(), "sklearn_flask_server.pid");
      String pidContents = Files.readFirstLine(pidFilePath.toFile(), Charset.defaultCharset());

      // Update the info in the db
      serving.setLocalPid(Integer.valueOf(pidContents));
      serving.setLocalPort(port);
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
