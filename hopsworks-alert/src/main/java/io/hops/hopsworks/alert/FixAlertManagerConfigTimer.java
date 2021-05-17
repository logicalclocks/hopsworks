/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class FixAlertManagerConfigTimer {
  private final static Logger LOGGER = Logger.getLogger(FixAlertManagerConfigTimer.class.getName());

  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  
  @Schedule(persistent = false, hour = "*")
  public void timer() {
    try {
      alertManagerConfiguration.restoreFromBackup();
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Failed to fix Alert Manager config from backup. " + e.getMessage());
    }
  }
}
