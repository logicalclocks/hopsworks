/*
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
 */

package io.hops.hopsworks.common.serving.tf;

import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.dao.serving.TfServingFacade;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static io.hops.hopsworks.common.serving.tf.LocalhostTfServingController.PID_STOPPED;
import static io.hops.hopsworks.common.serving.tf.LocalhostTfServingController.SERVING_DIRS;

/**
 * This singleton iterates over the running TfServing instances and checks whether
 * or not they are still alive.
 *
 * The monitor needs to run only if the TfServingController is the Localhost one
 */
@Singleton
@Startup
@DependsOn("Settings")
public class LocalhostTfServingMonitor {

  private final static Logger logger =
      Logger.getLogger(LocalhostTfServingMonitor.class.getName());

  private final static String PR_MATCHER = "tensorflow_mode";

  @Resource
  private TimerService timerService;

  @EJB
  private TfServingFacade tfServingFacade;
  @EJB
  private Settings settings;

  @Inject
  private TfServingController tfServingController;

  private String script;
  private Pattern pattern;

  @PostConstruct
  public void init() {
    if (tfServingController.getClassName().equals(LocalhostTfServingController.class.getName())) {
      // Setup the controller
      String rawInterval = settings.getTFServingMonitorInt();
      Long intervalValue = Settings.getConfTimeValue(rawInterval);
      TimeUnit intervalTimeunit = Settings.getConfTimeTimeUnit(rawInterval);
      logger.log(Level.INFO, "Localhost TfServing instances monitor is configure to run every" + intervalValue +
          " " + intervalTimeunit.name());

      intervalValue = intervalTimeunit.toMillis(intervalValue);
      timerService.createTimer(intervalValue, intervalValue, "Localhost TfServing instances monitor");

      script = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";
      pattern = Pattern.compile(PR_MATCHER);
    }

  }

  @Timeout
  public void monitor(Timer timer) {
    logger.log(Level.FINEST, "Run Localhost TfServing instances monitor");

    // Get the list of processes running on this machine
    Set<Integer> tfServingPSet = new HashSet<>();
    try {
      String line;
      Process p = Runtime.getRuntime().exec("ps -e");
      BufferedReader input =
          new BufferedReader(new InputStreamReader(p.getInputStream()));
      while ((line = input.readLine()) != null) {
        if (pattern.matcher(line).find()) {
          tfServingPSet.add(Integer.valueOf(line.split(" ")[0]));
        }
      }
      input.close();
    } catch (Exception err) {
      logger.log(Level.SEVERE, "Could not read the processes list");
    }

    // Get the list of running Localhost TfServing instances
    List<TfServing> tfServingList = tfServingFacade.getLocalhostRunning();
    for (TfServing tfServing : tfServingList) {
      try {
        TfServing dbTfServing = tfServingFacade.acquireLock(tfServing.getProject(), tfServing.getId());

        if (!tfServingPSet.contains(dbTfServing.getLocalPid())) {
          // The processes is dead, run the kill script to delete the directory
          // and update the value in the db
          Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + tfServing.getLocalDir());

          String[] command = {"/usr/bin/sudo", script, "kill", String.valueOf(dbTfServing.getLocalPid()),
              String.valueOf(dbTfServing.getLocalPort()), secretDir.toString()};

          logger.log(Level.INFO, Arrays.toString(command));
          ProcessBuilder pb = new ProcessBuilder(command);

          try {
            Process process = pb.start();
            process.waitFor();

            // If the process succeeded to delete the localDir update the db
            dbTfServing.setLocalPid(PID_STOPPED);
            dbTfServing.setLocalPort(-1);
            tfServingFacade.updateDbObject(dbTfServing, dbTfServing.getProject());
          } catch (IOException | InterruptedException e) {}
        }

        tfServingFacade.releaseLock(tfServing.getProject(), tfServing.getId());
      } catch (TfServingException e) {
        logger.log(Level.SEVERE, "Error proecessing TfServing instance with id: "
            + tfServing.getId(), e);
      }
    }
  }

}
