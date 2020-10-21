/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public interface JupyterManager {
  
  JupyterDTO startJupyterServer(Project project, String secretConfig, String hdfsUser, Users user,
    JupyterSettings js, String allowOrigin) throws ServiceException, JobException;
  
  String getJupyterHome(String hdfsUser, Project project, String secret) throws ServiceException;
  
  void projectCleanup(Logger logger, Project project);
  
  void waitForStartup(Project project, Users user) throws TimeoutException;
  
  void stopOrphanedJupyterServer(String cid, Integer port) throws ServiceException;
  
  void stopJupyterServer(Project project, Users user, String hdfsUsername, String jupyterHomePath, String cid,
    Integer port) throws ServiceException;
  
  void projectCleanup(Project project);
  
  boolean ping(JupyterProject jupyterProject);
  
  List<JupyterProject> getAllNotebooks();
  
  String getJupyterHost();
}
