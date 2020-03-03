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
package io.hops.hopsworks.common.dao.maggy;

import io.hops.hopsworks.common.livy.LivyController;
import io.hops.hopsworks.common.livy.LivyMsg;
import io.hops.hopsworks.common.livy.LivyMsg.Session;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@Singleton
public class MaggyCleaner {

  private final static Logger LOGGER = Logger.getLogger(
      MaggyCleaner.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds  
  @EJB
  private LivyController livyService;
  @EJB
  private MaggyFacade maggyFacade;
  @EJB
  private Settings settings;

  // Run once per hour
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {
    try {
      // Get all Running Maggy Drivers
      List<MaggyDriver> drivers = maggyFacade.getAllDrivers();
      List<MaggyDriver> driversToRemove = new ArrayList<>();

      if (drivers != null) {

        LivyMsg msg = livyService.getLivySessions();
        if (msg == null) {
          LOGGER.info("Maggy Cleaner could not contact Livy. Exiting....");
          return;
        }
        Session[] sessions = msg.getSessions();

        for (MaggyDriver md : drivers) {
        // Only cleanup Drivers older than 24 hours - in case Livy returns no session,
        // but there really is an Driver running
          if (md.getCreated().before(
              new Date(System.currentTimeMillis() - settings.getMaggyCleanupInterval()))) {
            driversToRemove.add(md);
            if (sessions != null) {
              for (Session s : sessions) {
                String h = s.getAppId();
                if (h != null) {
                  if (h.compareToIgnoreCase(md.getAppId()) == 0) {
                    driversToRemove.remove(md);  // don't remove Driver, app is still running.
                  }
                }
              }
            }
          }
        }

        for (MaggyDriver md : driversToRemove) {
          maggyFacade.remove(md);
        }

      } else {
        LOGGER.info("No Maggy Drivers running. Sleeping again.");
      }
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, "Got an exception while cleaning up maggy drivers", e);
    }
  }

}
