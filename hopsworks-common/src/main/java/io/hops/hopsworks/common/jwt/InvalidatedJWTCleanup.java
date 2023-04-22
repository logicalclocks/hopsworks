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
package io.hops.hopsworks.common.jwt;

import io.hops.hopsworks.jwt.JWTController;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class InvalidatedJWTCleanup {

  private final static Logger LOGGER = Logger.getLogger(InvalidatedJWTCleanup.class.getName());

  @EJB
  private JWTController jWTController;
  
  @Schedule(info = "Invalidated JWT cleanup")
  public void cleanInvalidatedJwt(Timer timer) {
    try {
      int count = jWTController.cleanupInvalidTokens();
      LOGGER.log(Level.INFO, "{0} timer event: {1}, removed {2} tokens.",
          new Object[]{timer.getInfo(), new Date(), count});
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Got an exception while cleaning up invalidated jwts", e);
    }
  }
}
