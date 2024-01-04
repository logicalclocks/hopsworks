/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.jobs.JobsMonitor;
import io.hops.hopsworks.common.jobs.execution.ExecutionUpdateController;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class YarnJobsMonitor implements JobsMonitor {

  private static final Logger LOGGER = Logger.getLogger(YarnJobsMonitor.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private ExecutionUpdateController executionUpdateController;
  @EJB
  private YarnExecutionFinalizer execFinalizer;
  @EJB
  private YarnMonitor yarnMonitor;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @EJB
  private YarnClientService yarnClientService;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 5000L; // 5 sec
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Yarn job monitor timer",
      false));
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  private int maxStatusPollRetry;
  
  Map<String, Integer> failures = new HashMap<>();
  private final Map<ApplicationId, Future<Execution>> copyLogsFutures = new HashMap<>();
  
  @Timeout
  public synchronized void yarnJobMonitor(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    YarnClientWrapper yarnClientWrapper = null;
    try {
      yarnClientWrapper = yarnClientService.getYarnClientSuper();
      Map<String, Execution> executions = new HashMap<>();
      List<Execution> execs = executionFacade.findNotFinished();
      if (execs != null && !execs.isEmpty()) {
        for (Execution exec : execs) {
          if (exec.getAppId() != null) {
            executions.put(exec.getAppId(), exec);
          }
        }
        maxStatusPollRetry = settings.getMaxStatusPollRetry();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Execution> entry : executions.entrySet()) {
          ApplicationId appId = ApplicationId.fromString(entry.getKey());
          Execution exec = internalMonitor(yarnClientWrapper.getYarnClient(), appId, executions.get(entry.getKey()));
          if (exec == null) {
            toRemove.add(entry.getKey());
          }
        }
        for (String appID : toRemove) {
          failures.remove(appID);
        }
        // This is here to do bookkeeping. Remove from the map all the executions which have finished copying the logs
        copyLogsFutures.entrySet().removeIf(futureResult -> futureResult.getValue().isDone());
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error while monitoring jobs", ex);
    } finally {
      yarnClientService.closeYarnClient(yarnClientWrapper);
    }
  }
  
  private Execution internalMonitor(YarnClient yarnClient, ApplicationId appId, Execution exec) {
    try {
      YarnApplicationState appState = yarnMonitor.getApplicationState(yarnClient, appId);
      FinalApplicationStatus finalAppStatus = yarnMonitor.getFinalApplicationStatus(yarnClient, appId);
      float progress = yarnMonitor.getProgress(yarnClient, appId);
      exec = updateProgress(progress, exec);
      exec = updateState(JobState.getJobState(appState), exec);
      exec = updateFinalStatus(JobFinalStatus.getJobFinalStatus(finalAppStatus), exec);
      
      if ((appState == YarnApplicationState.FAILED
          || appState == YarnApplicationState.FINISHED
          || appState == YarnApplicationState.KILLED)
          && !copyLogsFutures.containsKey(appId)) {
        
        exec = executionFacade.updateState(exec, JobState.AGGREGATING_LOGS);
        // Async call
        Future<Execution> futureResult = execFinalizer.copyLogs(exec);
        copyLogsFutures.put(appId, futureResult);
        return null;
      }
    } catch (IOException | YarnException ex) {
      Integer failure = failures.get(exec.getAppId());
      if (failure == null) {
        failure = 1;
      } else {
        failure++;
      }
      failures.put(exec.getAppId(), failure);
      LOGGER.log(Level.WARNING, "Failed to get application state for execution " + exec + ". Tried " + failures
          + " time(s).", ex);
    }
    if (failures.get(exec.getAppId()) != null && failures.get(exec.getAppId()) > maxStatusPollRetry) {
      try {
        LOGGER.log(Level.SEVERE, "Killing application, {0}, because unable to poll for status.", exec);
        yarnMonitor.cancelJob(yarnClient, appId);
        exec = updateFinalStatus(JobFinalStatus.KILLED, exec);
        exec = updateProgress(0, exec);
        execFinalizer.finalizeExecution(exec, JobState.KILLED);
      } catch (YarnException | IOException ex) {
        LOGGER.log(Level.SEVERE, "Failed to cancel execution, " + exec + " after failing to poll for status.", ex);
        execFinalizer.finalizeExecution(exec, JobState.FRAMEWORK_FAILURE);
      }
      return null;
    }
    return exec;
  }
  
  @Override
  public Execution updateProgress(float progress, Execution execution) {
    return executionUpdateController.updateProgress(progress, execution);
  }
  
  @Override
  public Execution updateState(JobState newState, Execution execution) {
    return executionUpdateController.updateState(newState, execution);
  }
  
  private Execution updateFinalStatus(JobFinalStatus finalStatus, Execution execution) {
    return executionUpdateController.updateFinalStatusAndSendAlert(finalStatus, execution);
  }
}