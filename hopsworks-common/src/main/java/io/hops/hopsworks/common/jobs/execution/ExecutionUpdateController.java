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
package io.hops.hopsworks.common.jobs.execution;

import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ExecutionUpdateController {
  private static final Logger LOGGER = Logger.getLogger(ExecutionUpdateController.class.getName());
  
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private AlertController alertController;
  
  public Execution updateProgress(float progress, Execution execution) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(execution.getId()).isPresent()) {
      execution = executionFacade.updateProgress(execution, progress);
    }
    return execution;
  }

  public Execution updateExecutionStop(long executionStop, Execution execution) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(execution.getId()).isPresent()) {
      execution =  executionFacade.updateExecutionStop(execution, executionStop);
    }
    return execution;
  }
  
  public Execution updateState(JobState newState, Execution execution) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(execution.getId()).isPresent()) {
      execution = executionFacade.updateState(execution, newState);
    }
    return execution;
  }
  
  public Execution updateStateAndSendAlert(Execution execution) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(execution.getId()).isPresent()) {
      execution = executionFacade.update(execution);
      alertController.sendAlert(execution.getState(), execution);
    }
    return execution;
  }
  
  public Execution updateFinalStatusAndSendAlert(JobFinalStatus finalStatus, Execution execution) {
    //The execution won't exist in the database, if the job has been deleted.
    if (executionFacade.findById(execution.getId()).isPresent()) {
      execution = executionFacade.updateFinalStatus(execution, finalStatus);
      alertController.sendAlert(finalStatus, execution);
    }
    return execution;
  }
}
