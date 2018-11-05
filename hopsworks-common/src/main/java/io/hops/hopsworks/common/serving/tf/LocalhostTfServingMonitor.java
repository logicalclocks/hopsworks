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
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class LocalhostTfServingMonitor {

  private final static Logger LOGGER =
      Logger.getLogger(LocalhostTfServingMonitor.class.getName());

  @Resource
  private TimerService timerService;

  @EJB
  private TfServingFacade tfServingFacade;
  @EJB
  private Settings settings;

  @Inject
  private TfServingController tfServingController;

  private String script;

  @PostConstruct
  public void init() {
    if (tfServingController.getClassName().equals(LocalhostTfServingController.class.getName())) {
      // Setup the controller
      String rawInterval = settings.getTFServingMonitorInt();
      Long intervalValue = settings.getConfTimeValue(rawInterval);
      TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
      LOGGER.log(Level.INFO, "Localhost TfServing instances monitor is configure to run every" + intervalValue +
          " " + intervalTimeunit.name());

      intervalValue = intervalTimeunit.toMillis(intervalValue);
      timerService.createTimer(intervalValue, intervalValue, "Localhost TfServing instances monitor");

      script = settings.getHopsworksDomainDir() + "/bin/tfserving.sh";
    }

  }

  @Timeout
  public void monitor(Timer timer) {
    LOGGER.log(Level.FINE, "Run Localhost TfServing instances monitor");

    // Get the list of running Localhost TfServing instances
    List<TfServing> tfServingList = tfServingFacade.getLocalhostRunning();
    for (TfServing tfServing : tfServingList) {
      try {
        TfServing dbTfServing = tfServingFacade.acquireLock(tfServing.getProject(), tfServing.getId());

        String[] aliveCommand = new String[]{"/usr/bin/sudo", script, "alive",
            String.valueOf(dbTfServing.getLocalPid()), dbTfServing.getLocalDir()};

        LOGGER.log(Level.FINE, Arrays.toString(aliveCommand));
        ProcessBuilder pb = new ProcessBuilder(aliveCommand);
        try {
          Process process = pb.start();
          process.waitFor();

          if (process.exitValue() != 0) {

            // The processes is dead, run the kill script to delete the directory
            // and update the value in the db
            Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + tfServing.getLocalDir());

            String[] killCommand = {"/usr/bin/sudo", script, "kill", String.valueOf(dbTfServing.getLocalPid()),
                String.valueOf(dbTfServing.getLocalPort()), secretDir.toString()};

            LOGGER.log(Level.FINE, Arrays.toString(killCommand));
            pb = new ProcessBuilder(killCommand);

            process = pb.start();
            process.waitFor();

            // If the process succeeded to delete the localDir update the db
            dbTfServing.setLocalPid(PID_STOPPED);
            dbTfServing.setLocalPort(-1);
            tfServingFacade.updateDbObject(dbTfServing, dbTfServing.getProject());
          }

        } catch (IOException | InterruptedException e) {
          LOGGER.log(Level.SEVERE, "Could not clean up TfServing instance with id: "
              + tfServing.getId(), e);
        }

        tfServingFacade.releaseLock(tfServing.getProject(), tfServing.getId());
      } catch (TfServingException e) {
        LOGGER.log(Level.INFO, "Error processing TfServing instance with id: "
            + tfServing.getId(), e);
      }
    }
  }
}
