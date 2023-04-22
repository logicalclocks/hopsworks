/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dataset.acl;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class PermissionsCleaner {
  
  private final static Logger LOGGER = Logger.getLogger(PermissionsCleaner.class.getName());
  
  @EJB
  private PermissionsFixer permissionsFixer;
  
  private int counter = 0;
  
  @Schedule(minute = "*/15", hour = "*", info = "Fix dataset permissions timer")
  public void fixDatasetPermissions(Timer timer) {
    counter = permissionsFixer.fixPermissions(counter, System.currentTimeMillis());
    LOGGER.log(Level.INFO, "Fix permissions triggered by timer counter={0}", counter);
  }
}
