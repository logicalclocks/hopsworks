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

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface JupyterManager {
  JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings js, String allowOrigin) throws ServiceException;
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default String getJupyterHome(Settings settings, String hdfsUser, Project project, String secret)
      throws ServiceException {
    
    if (project == null || secret == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_HOME_ERROR, Level.WARNING, "user: " + hdfsUser);
    }
    return settings.getJupyterDir() + File.separator
      + Settings.DIR_ROOT + File.separator + project.getName()
      + File.separator + hdfsUser + File.separator + secret;
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  default void projectCleanup(Settings settings, Logger logger, OSProcessExecutor osProcessExecutor, Project project) {
    String prog = settings.getSudoersDir() + "/jupyter-project-cleanup.sh";
    int exitValue;
    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand(project.getName());
    if (!logger.isLoggable(Level.FINE)) {
      pdBuilder.ignoreOutErrStreams(true);
    }

    try {
      ProcessResult processResult = osProcessExecutor.execute(pdBuilder.build());
      logger.log(Level.FINE, processResult.getStdout());
      exitValue = processResult.getExitCode();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Problem cleaning up project: "
          + project.getName() + ": {0}", ex.toString());
      exitValue = -2;
    }

    if (exitValue != 0) {
      logger.log(Level.WARNING, "Problem remove project's jupyter folder: "
          + project.getName());
    }
  }

  void waitForStartup(Project project, Users user) throws TimeoutException;

  void stopOrphanedJupyterServer(Long pid, Integer port) throws ServiceException;
  
  void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath, Long pid,
      Integer port) throws ServiceException;

  void projectCleanup(Project project);
  
  boolean ping(JupyterProject jupyterProject);
  
  List<JupyterProject> getAllNotebooks();
}
