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

package io.hops.hopsworks.common.jobs.execution;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ExecutionsCleaner {

  private final static Logger LOG = Logger.getLogger(ExecutionsCleaner.class.getName());

  private int batchSize=2;

  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade executionFacade;
  @Resource
  private TimerService timerService;

  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {
    batchSize = settings.getExecutionsCleanerBatchSize();
    timerService.createIntervalTimer(10L,
                                     settings.getExecutionsCleanerInterval(),
                                     new TimerConfig("ExecutionCleaner",
                                     false));
  }

  /**
   * Timer bean to periodically delete executions that do not belong to any jobs. There is not FK constraint between
   * the jobs and executions tables in the database and as a result executions need to be cleaned up periodically in
   * this timer. This is done to avoid having the database trying delete too many execution records when a job is
   * deleted which would results in transactions timeouts and potential database overload.
   *
   * We do not use orphanRemoval on the JPA entity to allow for a more flexible UX where there number of executions
   * for a job can grow to more than what the database can handle to remove in a single transaction. This timer accepts
   * a batch size that defines how many executions are to be deleted par trigger event. For example, if a job with
   * 10000 executions is deleted, the timer can remove the executions in batches of 1000.
   *
   * @param timer timer
   */
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 1000)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void deleteOrphanExecutions(Timer timer) {
    try {
      LOG.log(Level.FINE, "deleteOrphanExecutions start");
      List<Execution> executions = executionFacade.findOrphaned(new Pair<>(0, batchSize));
      while (!executions.isEmpty()) {
        LOG.log(Level.INFO, "Deleting orphaned executions: " + executions);
        executionFacade.batchDelete(executions);
        executions = executionFacade.findOrphaned(new Pair<>(0, batchSize));
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "ExecutionCleaner timer error", ex);
    }
    LOG.log(Level.FINE, "deleteOrphanExecutions end");
  }
}
