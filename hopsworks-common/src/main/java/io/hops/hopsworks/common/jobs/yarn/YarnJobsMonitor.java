/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;

@Singleton
@DependsOn("Settings")
public class YarnJobsMonitor {

  private static final Logger LOG = Logger.getLogger(YarnJobsMonitor.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private YarnExecutionFinalizer execFinalizer;
  @EJB
  private YarnClientService ycs;

  private int maxStatusPollRetry;

  Map<String, Execution> executions = new HashMap<>();
  Map<String, YarnMonitor> monitors = new HashMap<>();
  Map<String, Integer> failures = new HashMap<>();
  boolean init = true;
  private List<CopyLogsFutureResult> copyLogsFutures = new ArrayList<>();
  
  /**
   * Add an execution and its monitor to the applications that need to be monitored.
   * <p/>
   * @param appId the id of the application to monitor
   * @param exec the execution corresponding to the monitored application
   * @param monitor the monitor for this application
   */
  public void addToMonitor(String appId, Execution exec, YarnMonitor monitor) {
    monitor = monitor.start();
    executions.put(appId, exec);
    monitors.put(appId, monitor);
  }

  @Schedule(persistent = false,
      second = "*",
      minute = "*",
      hour = "*")
  synchronized public void monitor(Timer timer) {
    if (init) {
      List<Execution> execs = executionFacade.findAllNotFinished();
      if (execs != null) {
        for (Execution exec : execs) {
          if (exec.getAppId() != null) {
            executions.put(exec.getAppId(), exec);
          }
        }
      }
      maxStatusPollRetry = settings.getMaxStatusPollRetry();
      init = false;
    }
    List<String> toRemove = new ArrayList<>();
    List<Execution> toUpdate = new ArrayList<>();
    for (String appID : executions.keySet()) {
      YarnMonitor monitor = monitors.get(appID);
      if (monitor == null) {
        ApplicationId appId = ConverterUtils.toApplicationId(appID);
        YarnClientWrapper newYarnclientWrapper = ycs.getYarnClientSuper(settings
            .getConfiguration());
        monitor = new YarnMonitor(appId, newYarnclientWrapper, ycs);
        monitors.put(appID, monitor);
      }
      Execution exec = internalMonitor(executions.get(appID), monitor);
      if (exec != null) {
        toUpdate.add(exec);
      } else {
        toRemove.add(appID);
        monitor.close();
      }
    }
    for (Execution exec : toUpdate) {
      executions.put(exec.getAppId(), exec);
    }
    for (String appID : toRemove) {
      executions.remove(appID);
      failures.remove(appID);
      monitors.remove(appID);
    }
  
    Iterator<CopyLogsFutureResult> futureResultIter = copyLogsFutures.iterator();
    while (futureResultIter.hasNext() ){
      CopyLogsFutureResult futureResult = futureResultIter.next();
      if (futureResult.execFuture.isDone()) {
        try {
          execFinalizer.finalize(futureResult.execFuture.get(),
              futureResult.jobState);
        } catch (ExecutionException | InterruptedException ex) {
          LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        futureResultIter.remove();
      }
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

      if (appState == YarnApplicationState.FAILED || appState == YarnApplicationState.FINISHED || appState
          == YarnApplicationState.KILLED) {
        exec = executionFacade.updateState(exec, JobState.AGGREGATING_LOGS);
        // Async call
        Future<Execution> futureResult = execFinalizer.copyLogs(exec);
        copyLogsFutures.add(new CopyLogsFutureResult(futureResult,
            JobState.getJobState(appState)));
        
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
      LOG.log(Level.WARNING, "Failed to get application state for execution " + exec + ". Tried " + failures
          + " time(s).", ex);
    }
    if (failures.get(exec.getAppId()) != null && failures.get(exec.getAppId()) > maxStatusPollRetry) {
      try {
        LOG.log(Level.SEVERE, "Killing application, {0}, because unable to poll for status.", exec);
        monitor.cancelJob(monitor.getApplicationId().toString());
        exec = updateState(JobState.KILLED, exec);
        exec = updateFinalStatus(JobFinalStatus.KILLED, exec);
        exec = updateProgress(0, exec);
        execFinalizer.finalize(exec, JobState.KILLED);
      } catch (YarnException | IOException ex) {
        LOG.
            log(Level.SEVERE, "Failed to cancel execution, " + exec + " after failing to poll for status.", ex);
        exec = updateState(JobState.FRAMEWORK_FAILURE, exec);
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
  
  private class CopyLogsFutureResult {
    private final Future<Execution> execFuture;
    private final JobState jobState;
    
    private CopyLogsFutureResult(Future<Execution> execFuture,
        JobState jobState) {
      this.execFuture = execFuture;
      this.jobState = jobState;
    }
  }
}