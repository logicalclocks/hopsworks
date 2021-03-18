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

package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JupyterManagerImpl implements JupyterManager {
  
  @EJB
  private Settings settings;
  
  @EJB
  private OSProcessExecutor osProcessExecutor;
  
  public abstract JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings js, String allowOrigin) throws ServiceException, JobException;
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String getJupyterHome(String hdfsUser, Project project, String secret)
      throws ServiceException {
    
    if (project == null || secret == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_HOME_ERROR, Level.WARNING, "user: " + hdfsUser);
    }
    return settings.getJupyterDir() + File.separator
      + Settings.DIR_ROOT + File.separator + project.getName()
      + File.separator + hdfsUser + File.separator + secret;
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void projectCleanup(Logger logger, Project project) {
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

  public abstract void waitForStartup(Project project, Users user) throws TimeoutException;
  
  public abstract void stopOrphanedJupyterServer(String cid, Integer port) throws ServiceException;
  
  public abstract void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath,
    String cid, Integer port) throws ServiceException;
  
  public abstract void projectCleanup(Project project);
  
  public abstract boolean ping(JupyterProject jupyterProject);
  
  public abstract List<JupyterProject> getAllNotebooks();
  
  public abstract String getJupyterHost();
}
