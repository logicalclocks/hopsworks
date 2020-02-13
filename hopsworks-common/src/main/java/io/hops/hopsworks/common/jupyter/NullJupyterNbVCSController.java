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
import io.hops.hopsworks.common.integrations.NullJupyterNbVCSStereotype;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collections;
import java.util.Set;

@NullJupyterNbVCSStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullJupyterNbVCSController implements JupyterNbVCSController {
  public static final RepositoryStatus EMPTY_REPOSITORY_STATUS = new RepositoryStatus.UnmodifiableRepositoryStatus();
  
  @Override
  public JupyterContentsManager getJupyterContentsManagerClass(String remoteURI) throws ServiceException {
    return JupyterContentsManager.HDFS_CONTENTS_MANAGER;
  }
  
  @Override
  public boolean isGitAvailable() {
    return false;
  }
  
  @Override
  public Set<String> getRemoteBranches(Users user, String apiKeyName, String remoteURI) throws ServiceException {
    return Collections.emptySet();
  }
  
  @Override
  public String getGitApiKey(String hdfsUser, String apiKeyName) throws ServiceException{
    return "";
  }
  
  @Override
  public RepositoryStatus init(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws ServiceException {
    return EMPTY_REPOSITORY_STATUS;
  }
  
  @Override
  public RepositoryStatus status(JupyterProject jupyterProject, JupyterSettings jupyterSettings)
      throws ServiceException {
    return EMPTY_REPOSITORY_STATUS;
  }
  
  @Override
  public RepositoryStatus pull(JupyterProject jupyterProject, JupyterSettings jupyterSettings)
      throws ServiceException {
    return EMPTY_REPOSITORY_STATUS;
  }
  
  @Override
  public RepositoryStatus push(JupyterProject jupyterProject, JupyterSettings jupyterSettings, Users user)
      throws ServiceException {
    return EMPTY_REPOSITORY_STATUS;
  }
}
