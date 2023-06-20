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

import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.jwt.JWTController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class InvalidatedJWTCleanup {

  private final static Logger LOGGER = Logger.getLogger(InvalidatedJWTCleanup.class.getName());

  @EJB
  private JWTController jWTController;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 24*3600000L; // 24 hour
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Invalidated JWT cleanup",
      false));
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Timeout
  public void cleanInvalidatedJwt(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    
    try {
      int count = jWTController.cleanupInvalidTokens();
      LOGGER.log(Level.INFO, "{0} timer event: {1}, removed {2} tokens.",
          new Object[]{timer.getInfo(), new Date(), count});
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Got an exception while cleaning up invalidated jwts", e);
    }
  }
}
