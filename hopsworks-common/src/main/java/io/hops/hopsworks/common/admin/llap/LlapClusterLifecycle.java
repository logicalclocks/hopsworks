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

import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class LlapClusterLifecycle {

  @EJB
  private Settings settings;
  @EJB
  private VariablesFacade variablesFacade;
  @EJB
  private LlapClusterFacade llapClusterFacade;
  @EJB
  private YarnClientService yarnClientService;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;

  private static final Logger LOGGER = Logger.getLogger(LlapClusterLifecycle.class.getName());

  private Set<String> applicationTypeSet = null;
  private EnumSet<YarnApplicationState> applicationStateEnumSet = null;
  private Set<String> hiveUser = new HashSet<>();

  @PostConstruct
  private void init() {
    applicationTypeSet = new HashSet<>(Arrays.asList("org-apache-slider"));
    applicationStateEnumSet = yarnApplicationstateFacade.getRunningStates();
    hiveUser.add(settings.getHiveSuperUser());
  }

  @Lock(LockType.WRITE)
  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void startCluster(int nInstances, long execMemory, long cacheMemory, int nExecutors,
                           int nIOThreads) {
    // Check that the cluster isn't already up or that the cluster isn't already starting
    if (llapClusterFacade.isClusterUp()){
      return ;
    }

    // Store fake pid to make the waitForCluster function working
    variablesFacade.storeVariable(Settings.VARIABLE_LLAP_START_PROC, "-2");

    // Save new configuration in the database
    llapClusterFacade.saveConfiguration(nInstances, execMemory, cacheMemory, nExecutors,
        nIOThreads);

    // Script path
    String startScript = settings.getSudoersDir() + "/start-llap.sh";

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand("-u")
        .addCommand(settings.getHiveSuperUser())
        .addCommand(startScript)
        .addCommand(String.valueOf(nInstances))
        .addCommand(String.valueOf(execMemory))
        .addCommand(String.valueOf(cacheMemory))
        .addCommand(String.valueOf(nExecutors))
        .addCommand(String.valueOf(nIOThreads))
        .redirectErrorStream(true)
        .setWaitTimeout(5L, TimeUnit.MINUTES)
        .build();

    YarnClientWrapper yarnClientWrapper = null;
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        LOGGER.log(Level.INFO, "LLAP output: " + processResult.getStdout());
      }

      // Store the appId in the database
      yarnClientWrapper = yarnClientService.getYarnClientSuper(settings.getConfiguration());
      YarnClient yarnClient = yarnClientWrapper.getYarnClient();

      Set<String> queueSet = new HashSet<>();
      List<QueueInfo> queueInfoList = yarnClient.getAllQueues();
      queueInfoList.forEach(queue -> queueSet.add(queue.getQueueName()));

      List<ApplicationReport> appReports = yarnClient.getApplications(
          queueSet, hiveUser, applicationTypeSet, applicationStateEnumSet);

      // Search for LLAP application
      if (!appReports.isEmpty()) {
        variablesFacade.storeVariable(Settings.VARIABLE_LLAP_APP_ID, appReports.get(0).getApplicationId().toString());
      }
    } catch (IOException | YarnException e) {
      LOGGER.log(Level.SEVERE, "Error starting LLAP", e);
    } finally {
      // Process ended, clean the db
      variablesFacade.storeVariable(Settings.VARIABLE_LLAP_START_PROC, "-1");

      yarnClientService.closeYarnClient(yarnClientWrapper);
    }
  }


  @Lock(LockType.WRITE)
  public boolean stopCluster() {
    String llapAppID = variablesFacade.getVariableValue(Settings.VARIABLE_LLAP_APP_ID);
    if (llapAppID == null || llapAppID.isEmpty()) {
      return false;
    }

    ApplicationId appId = ApplicationId.fromString(llapAppID);
    YarnClient yarnClient = yarnClientService.getYarnClientSuper(settings.getConfiguration()).getYarnClient();
    try {
      yarnClient.killApplication(appId);
    } catch (IOException | YarnException e) {
      LOGGER.log(Level.SEVERE, "Could not kill llap cluster with appId: " + appId.toString(), e);
      return false;
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    return true;
  }

}
