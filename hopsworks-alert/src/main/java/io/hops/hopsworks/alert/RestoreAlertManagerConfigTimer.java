/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.alert;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class RestoreAlertManagerConfigTimer {
  private final static Logger LOGGER = Logger.getLogger(RestoreAlertManagerConfigTimer.class.getName());

  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;

  @PostConstruct
  public void init() {
    try {
      alertManagerConfiguration.restoreFromDb();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to restore Alert Manager config from database backup. " + e.getMessage());
    }
  }

  @Schedule(persistent = false, hour = "*", info = "Restore Alert Manager config from backup.")
  public void restoreAlertManagerConfigTimer() {
    try {
      alertManagerConfiguration.restoreFromBackup();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to fix Alert Manager config from backup. " + e.getMessage());
      e.printStackTrace();
    }
  }
}
