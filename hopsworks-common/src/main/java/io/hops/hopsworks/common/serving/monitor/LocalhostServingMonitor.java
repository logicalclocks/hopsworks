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

package io.hops.hopsworks.common.serving.monitor;

import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.dao.serving.ServingFacade;
import io.hops.hopsworks.common.dao.serving.ServingType;
import io.hops.hopsworks.common.serving.LocalhostServingController;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.serving.LocalhostServingController.PID_STOPPED;
import static io.hops.hopsworks.common.serving.LocalhostServingController.SERVING_DIRS;

/**
 * This singleton iterates over the running Serving instances and checks whether
 * or not they are still alive.
 * <p>
 * The monitor needs to run only if the ServingController is the Localhost one
 */
@Singleton
@Startup
public class LocalhostServingMonitor {

  private final static Logger LOGGER =
      Logger.getLogger(LocalhostServingMonitor.class.getName());

  @Resource
  private TimerService timerService;

  @EJB
  private ServingFacade servingFacade;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  @Inject
  private ServingController servingController;

  private String tfScript;
  private String sklearnScript;

  @PostConstruct
  public void init() {
    if (servingController.getClassName().equals(LocalhostServingController.class.getName())) {
      // Setup the controller
      String rawInterval = settings.getServingMonitorInt();
      Long intervalValue = settings.getConfTimeValue(rawInterval);
      TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
      LOGGER.log(Level.INFO, "Localhost Serving instances monitor is configure to run every" + intervalValue +
          " " + intervalTimeunit.name());

      intervalValue = intervalTimeunit.toMillis(intervalValue);
      timerService.createTimer(intervalValue, intervalValue, "Localhost Serving instances monitor");

      tfScript = settings.getSudoersDir() + "/tfserving.sh";
      sklearnScript = settings.getSudoersDir() + "/sklearn_serving.sh";
    }

  }

  @Timeout
  public void monitor(Timer timer) {
    try {
      // Get the list of running Localhost Serving instances
      List<Serving> servingList = servingFacade.getLocalhostRunning();
      for (Serving serving : servingList) {
        try {
          Serving dbServing = servingFacade.acquireLock(serving.getProject(), serving.getId());
          ProcessDescriptor.Builder builder = new ProcessDescriptor.Builder().addCommand("/usr/bin/sudo");
          if (serving.getServingType() == ServingType.TENSORFLOW) {
            builder.addCommand(tfScript);
          }
          if (serving.getServingType() == ServingType.SKLEARN) {
            builder.addCommand(sklearnScript);
          }
          ProcessDescriptor processDescriptor = builder.addCommand("alive")
              .addCommand(String.valueOf(dbServing.getLocalPid()))
              .addCommand(dbServing.getLocalDir())
              .ignoreOutErrStreams(true)
              .build();

          LOGGER.log(Level.FINE, processDescriptor.toString());
          try {
            ProcessResult processResult = osProcessExecutor.execute(processDescriptor);

            if (processResult.getExitCode() != 0) {

              // The processes is dead, run the kill script to delete the directory
              // and update the value in the db
              Path secretDir = Paths.get(settings.getStagingDir(), SERVING_DIRS + serving.getLocalDir());
              builder = new ProcessDescriptor.Builder().addCommand("/usr/bin/sudo");
              if (serving.getServingType() == ServingType.TENSORFLOW) {
                builder.addCommand(tfScript);
              }
              if (serving.getServingType() == ServingType.SKLEARN) {
                builder.addCommand(sklearnScript);
              }
              processDescriptor = builder.addCommand("kill")
                  .addCommand(String.valueOf(dbServing.getLocalPid()))
                  .addCommand(String.valueOf(dbServing.getLocalPort()))
                  .addCommand(secretDir.toString())
                  .ignoreOutErrStreams(true)
                  .build();

              LOGGER.log(Level.FINE, processDescriptor.toString());
              osProcessExecutor.execute(processDescriptor);

              // If the process succeeded to delete the localDir update the db
              dbServing.setLocalPid(PID_STOPPED);
              dbServing.setLocalPort(-1);
              servingFacade.updateDbObject(dbServing, dbServing.getProject());
            }

          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not clean up serving instance with id: "
                + serving.getId(), e);
          }

          servingFacade.releaseLock(serving.getProject(), serving.getId());
        } catch (ServingException e) {
          LOGGER.log(Level.INFO, "Error processing serving instance with id: "
              + serving.getId(), e);
        }
      }
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Got an exception while monitoring servings" , e);
    }
  }
}
