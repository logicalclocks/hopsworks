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

package io.hops.hopsworks.common.admin.llap;

import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptReport;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ContainerReport;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.YarnApplicationAttemptState;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class LlapClusterFacade {

  private static final Logger logger = Logger.getLogger(LlapClusterFacade.class.getName());

  private static final String NINSTANCES = "llap_ninstances";
  private static final String EXECMEMORY = "llap_exec_memory";
  private static final String CACHEMEMORY = "llap_cache_memory";
  private static final String NEXECUTORS = "llap_executors_threads";
  private static final String NIOTHREADS = "llap_io_threads";

  @EJB
  private YarnClientService yarnClientService;
  @EJB
  private Settings settings;
  @EJB
  private VariablesFacade variablesFacade;

  public LlapClusterStatus getClusterStatus() {
    LlapClusterStatus clusterStatus = new LlapClusterStatus();
    if (isClusterStarting()) {
      clusterStatus.setClusterStatus(LlapClusterStatus.Status.LAUNCHING);
    } else if (isClusterUp()) {
      clusterStatus.setClusterStatus(LlapClusterStatus.Status.UP);
      clusterStatus.setHosts(getLlapHosts());
    } else {
      clusterStatus.setClusterStatus(LlapClusterStatus.Status.DOWN);
    }

    String nInstances = variablesFacade.getVariableValue(NINSTANCES);
    if (nInstances != null) {
      clusterStatus.setInstanceNumber(Integer.parseInt(nInstances));
    }
    String execMemory = variablesFacade.getVariableValue(EXECMEMORY);
    if (execMemory != null) {
      clusterStatus.setExecutorsMemory(Long.parseLong(execMemory));
    }
    String cacheMemory = variablesFacade.getVariableValue(CACHEMEMORY);
    if (cacheMemory != null) {
      clusterStatus.setCacheMemory(Long.parseLong(cacheMemory));
    }
    String nExecutors = variablesFacade.getVariableValue(NEXECUTORS);
    if (nExecutors != null) {
      clusterStatus.setExecutorsPerInstance(Integer.parseInt(nExecutors));
    }
    String nIOThreads = variablesFacade.getVariableValue(NIOTHREADS);
    if (nIOThreads != null) {
      clusterStatus.setIOThreadsPerInstance(Integer.parseInt(nIOThreads));
    }

    return clusterStatus;
  }

  public boolean isClusterUp() {
    String llapAppID = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_APP_ID);
    if (llapAppID == null || llapAppID.isEmpty()) {
      return false;
    }

    ApplicationId appId = ApplicationId.fromString(llapAppID);
    YarnClient yarnClient = yarnClientService.getYarnClientSuper(settings.getConfiguration()).getYarnClient();
    ApplicationReport applicationReport = null;
    try {
      applicationReport = yarnClient.getApplicationReport(appId);
    } catch (IOException | YarnException e) {
      logger.log(Level.SEVERE, "Could not retrieve application state for llap cluster with appId: "
          + appId.toString(), e);
      return false;
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    YarnApplicationState appState = applicationReport.getYarnApplicationState();
    return appState == YarnApplicationState.RUNNING ||
        appState == YarnApplicationState.SUBMITTED ||
        appState == YarnApplicationState.ACCEPTED ||
        appState == YarnApplicationState.NEW ||
        appState == YarnApplicationState.NEW_SAVING;
  }


  public boolean isClusterStarting() {
    String pidString = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_START_PROC);
    long pid = -1;
    if (pidString != null) {
      pid = Long.valueOf(pidString);
    }

    if (pid == -1) {
      return false;
    } else if (pid == -2) {
      return true;
    } else {
      // Check if the process is still running
      File procDir = new File("/proc/" + String.valueOf(pid));
      return procDir.exists();
    }
  }

  public List<String> getLlapHosts() {
    ArrayList<String> hosts = new ArrayList<>();

    if (!isClusterUp() || isClusterStarting()) {
      return hosts;
    }

    // The cluster is app, so the appId exists
    String llapAppID = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_APP_ID);

    ApplicationId appId = ApplicationId.fromString(llapAppID);
    YarnClient yarnClient = yarnClientService.getYarnClientSuper(settings.getConfiguration()).getYarnClient();
    try {
      List<ApplicationAttemptReport> attempts = yarnClient.getApplicationAttempts(appId);
      ApplicationAttemptReport current = null;
      for (ApplicationAttemptReport attempt : attempts) {
        // Only if the app is running the metrics are available
        if (attempt.getYarnApplicationAttemptState() == YarnApplicationAttemptState.RUNNING) {
          current = attempt;
          break;
        }
      }

      if (current == null) {
        return hosts;
      }

      List<ContainerReport> containerReports = yarnClient.getContainers(current.getApplicationAttemptId());

      // For all the new/running containers, which are not the application master, get the host
      for (ContainerReport containerReport : containerReports) {
        // Only if the container is running the metrics are available
        if (containerReport.getContainerState() == ContainerState.RUNNING &&
            !containerReport.getContainerId().equals(current.getAMContainerId())) {
          hosts.add(containerReport.getAssignedNode().getHost());
        }
      }

    } catch (IOException | YarnException ex) {
      logger.log(Level.SEVERE, "Couldn't retrieve the containers for LLAP cluster", ex);
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    return hosts;
  }

  public void saveConfiguration(int nInstances, long execMemory, long cacheMemory, int nExecutors,
                                int nIOThreaads) {
    variablesFacade.storeVariable(NINSTANCES, String.valueOf(nInstances));
    variablesFacade.storeVariable(EXECMEMORY, String.valueOf(execMemory));
    variablesFacade.storeVariable(CACHEMEMORY, String.valueOf(cacheMemory));
    variablesFacade.storeVariable(NEXECUTORS, String.valueOf(nExecutors));
    variablesFacade.storeVariable(NIOTHREADS, String.valueOf(nIOThreaads));
  }
}
