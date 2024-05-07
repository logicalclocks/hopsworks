/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.git;

import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Level;

@Stateless
@LocalBean
public class AsynchronousGitCommandExecutor {
  @Inject
  private CommandExecutor commandExecutor;

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(GitOpExecution gitOpExecution, BasicAuthSecrets authSecrets) throws GitOpException,
    HopsSecurityException {
    try {
      commandExecutor.execute(gitOpExecution, authSecrets);
    } catch (Exception ex) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.GIT_OPERATION_ERROR, Level.SEVERE, ex.getMessage());
    }
  }

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cancelGitExecution(GitOpExecution execution, String message) {
    commandExecutor.cancelGitExecution(execution, message);
  }
}
