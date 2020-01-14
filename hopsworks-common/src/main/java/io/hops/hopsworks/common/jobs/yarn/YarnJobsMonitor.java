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

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;

@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class YarnJobsMonitor {

  private static final Logger LOGGER = Logger.getLogger(YarnJobsMonitor.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private YarnExecutionFinalizer execFinalizer;
  @EJB
  private YarnClientService ycs;

  private int maxStatusPollRetry;

  Map<String, YarnMonitor> monitors = new HashMap<>();
  Map<String, Integer> failures = new HashMap<>();
  private final Map<ApplicationId, Future<Execution>> copyLogsFutures = new HashMap<>();

  @Schedule(persistent = false,
      second = "*/5",
      minute = "*",
      hour = "*")
  public synchronized void monitor(Timer timer) {
    try {
      Map<String, Execution> executions = new HashMap<>();
      List<Execution> execs = executionFacade.findNotFinished();
      if (execs != null && !execs.isEmpty()) {
        for (Execution exec : execs) {
          if (exec.getAppId() != null) {
            executions.put(exec.getAppId(), exec);
          }
        }
        //Remove (Close) all monitors of deleted jobs
        Iterator<Map.Entry<String, YarnMonitor>> monitorsIter = monitors.entrySet().iterator();
        while (monitorsIter.hasNext()) {
          Map.Entry<String, YarnMonitor> entry = monitorsIter.next();
          // Check if Value associated with Key is 10
          if (!executions.keySet().contains(entry.getKey())) {
            // Remove the element
            entry.getValue().close();
            monitorsIter.remove();
          }
        }
        maxStatusPollRetry = settings.getMaxStatusPollRetry();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Execution> entry : executions.entrySet()) {
          YarnMonitor monitor = monitors.get(entry.getKey());
          if (monitor == null) {
            ApplicationId appId = ApplicationId.fromString(entry.getKey());
            YarnClientWrapper newYarnclientWrapper = ycs.getYarnClientSuper(settings
              .getConfiguration());
            monitor = new YarnMonitor(appId, newYarnclientWrapper, ycs);
            monitors.put(entry.getKey(), monitor);
          }
          Execution exec = internalMonitor(executions.get(entry.getKey()), monitor);
          if (exec == null) {
            toRemove.add(entry.getKey());
            monitor.close();
          }
        }
        for (String appID : toRemove) {
          failures.remove(appID);
          monitors.remove(appID);
        }
        // This is here to do bookkeeping. Remove from the map all the executions which have finished copying the logs
        copyLogsFutures.entrySet().removeIf(futureResult -> futureResult.getValue().isDone());
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error while monitoring jobs", ex);
    }
  }
  
  private Execution internalMonitor(Execution exec, YarnMonitor monitor) {
    try {
      YarnApplicationState appState = monitor.getApplicationState();
      FinalApplicationStatus finalAppStatus = monitor.getFinalApplicationStatus();
      float progress = monitor.getProgress();
      exec = updateProgress(progress, exec);
      exec = updateState(JobState.getJobState(appState), exec);
      exec = updateFinalStatus(JobFinalStatus.getJobFinalStatus(finalAppStatus), exec);
      
      if ((appState == YarnApplicationState.FAILED
          || appState == YarnApplicationState.FINISHED
          || appState == YarnApplicationState.KILLED)
          && !copyLogsFutures.containsKey(monitor.getApplicationId())) {
        
        exec = executionFacade.updateState(exec, JobState.AGGREGATING_LOGS);
        // Async call
        Future<Execution> futureResult = execFinalizer.copyLogs(exec);
        copyLogsFutures.put(monitor.getApplicationId(), futureResult);
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
        monitor.cancelJob(monitor.getApplicationId().toString());
        exec = updateFinalStatus(JobFinalStatus.KILLED, exec);
        exec = updateProgress(0, exec);
        execFinalizer.finalize(exec, JobState.KILLED);
      } catch (YarnException | IOException ex) {
        LOGGER.log(Level.SEVERE, "Failed to cancel execution, " + exec + " after failing to poll for status.", ex);
        execFinalizer.finalize(exec, JobState.FRAMEWORK_FAILURE);
      }
      return null;
    }
    return exec;
  }

  private Execution updateProgress(float progress, Execution execution) {
    return executionFacade.updateProgress(execution, progress);
  }

  private Execution updateState(JobState newState, Execution execution) {
    return executionFacade.updateState(execution, newState);
  }

  private Execution updateFinalStatus(JobFinalStatus finalStatus, Execution execution) {
    return executionFacade.updateFinalStatus(execution, finalStatus);
  }
}