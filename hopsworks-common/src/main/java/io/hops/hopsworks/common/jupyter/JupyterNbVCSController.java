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

import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.exceptions.ServiceException;

import java.util.Set;

public interface JupyterNbVCSController {
  boolean isGitAvailable();
  JupyterContentsManager getJupyterContentsManagerClass(String remoteURI) throws ServiceException;
  Set<String> getRemoteBranches(Users user, String apiKeyName, String remoteURI) throws ServiceException;
  String getGitApiKey(String hdfsUser, String apiKeyName) throws ServiceException;
  
  RepositoryStatus init(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws ServiceException;
  RepositoryStatus status(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws ServiceException;
  RepositoryStatus pull(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws ServiceException;
  RepositoryStatus push(JupyterProject jupyterProject, JupyterSettings jupyterSettings, Users user)
      throws ServiceException;
}
