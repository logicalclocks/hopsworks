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
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static final Logger logger = Logger.getLogger(LlapClusterLifecycle.class.getName());

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
    String startScript = settings.getHopsworksDomainDir() + "/bin/start-llap.sh";

    // Regex for appId
    Pattern appIdPattern = Pattern.compile("application_[0-9]*_[0-9]*");
    String llapAppId = null;

    String[] command = {"/usr/bin/sudo", "-u", settings.getHiveSuperUser(),
        startScript,
        String.valueOf(nInstances),
        String.valueOf(execMemory),
        String.valueOf(cacheMemory),
        String.valueOf(nExecutors),
        String.valueOf(nIOThreads)};

    StringBuilder processOutput = new StringBuilder();

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    try {
      // Start the script
      Process proc = pb.start();

      // Get the pid of the process and store it in the database
      long pid = getProcessPid(proc);
      variablesFacade.storeVariable(Settings.VARIABLE_LLAP_START_PROC, String.valueOf(pid));

      // Parse the output
      BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String line;
      while ((line = br.readLine()) != null ) {
        // Store output for logging in case of exception
        processOutput.append(line).append('\n');

        // Look for application_clustertimestamp_appid in the output of the script
        Matcher matcher = appIdPattern.matcher(line);
        if (matcher.find()){
          llapAppId = matcher.group();
        }
      }

      if (llapAppId == null) {
        // For some reason the script failed to start the llap Cluster.
        // Dump the output of the script in the logs.
        logger.log(Level.SEVERE, "Could not start Hive LLAP cluster. Script output: " +
            processOutput.toString());
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Could not start Hive LLAP cluster. Script output: " +
          processOutput.toString(), ex);
    }

    // Store the appId in the database
    if (llapAppId != null) {
      variablesFacade.storeVariable(Settings.VARIABLE_LLAP_APP_ID, llapAppId);
    }

    // Process ended, clean the db
    variablesFacade.storeVariable(Settings.VARIABLE_LLAP_START_PROC, "-1");
  }

    /**
   * This only works on Linux systems. From Java 9, you can just call
   * p.getPid();
   * http://stackoverflow.com/questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
   *
   * @param proc
   * @return
   */
  private long getProcessPid(Process proc) {
    long pid = -1;

    try {
      Field f = proc.getClass().getDeclaredField("pid");
      f.setAccessible(true);
      pid = f.getLong(proc);
      f.setAccessible(false);
    } catch (Exception e) {
      pid = -1;
    }
    return pid;
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
      logger.log(Level.SEVERE, "Could not kill llap cluster with appId: " + appId.toString(), e);
      return false;
    } finally {
      try {
        yarnClient.close();
      } catch (IOException ex) {}
    }

    return true;
  }

}
