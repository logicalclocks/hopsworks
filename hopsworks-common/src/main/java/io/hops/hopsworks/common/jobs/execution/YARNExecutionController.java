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

package io.hops.hopsworks.common.jobs.execution;

import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Takes care of booting the execution of a job.
 */
@Stateless
@LocalhostStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class YARNExecutionController extends AbstractExecutionController implements ExecutionController {
  
  @Override
  public Execution start(Jobs job, String args, Users user)
    throws JobException, GenericException, ServiceException, ProjectException {
    return super.start(job, args, user);
  }
  
  @Override
  public void delete(Execution execution) throws JobException {
    if (!execution.getState().isFinalState()) {
      stopExecution(execution);
    }
    super.delete(execution);
  }
}
