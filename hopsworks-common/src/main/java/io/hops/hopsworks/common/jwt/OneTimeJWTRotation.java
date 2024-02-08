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
import io.hops.hopsworks.jwt.Constants;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class OneTimeJWTRotation {
  private final static Logger LOGGER = Logger.getLogger(OneTimeJWTRotation.class.getName());

  @EJB
  private JWTController jWTController;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  TimerService timerService;
  private Timer timer;
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = TimeUnit.HOURS.toMillis(24);

    // The Info of the Timer **MUST** start with 'Mark'
    // Read comment of the @Timeout annotated method below
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Mark old one-time JWT signing key",
        false));
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  /**
   * Recurring Timer method which rotates the one-time JWT signing key.
   * It will rename the current to *_old and generate a new one
   * During validation of a JWT we lookup the signing key based on the ID,
   * so even if the name changes we will still be able to validate "older" JWTs
   *
   * Once we have generated a new signing key and renamed the old we create
   * a single action Timer which fires a little bit later. The reason is that we
   * want sessions which are using the old signing key to still be able to authenticate
   * for a short-while.
   *
   * Since we cannot have two methods in the same EJB with @Timeout annotation we take
   * different paths in the same method based on the **Info** field of the Timer.
   * The recurring Timer's Info will/must start with 'Mark' while the single action
   * will/must start with 'Sweep'
   */
  @Timeout
  public void rotateOneTimeJWTSigningKey(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      LOGGER.log(Level.INFO, "I am not the Primary or Active region. Skip rotating one-time JWT signing key");
      return;
    }

    String timerInfo = (String) timer.getInfo();
    if (timerInfo.startsWith("Mark")) {
      LOGGER.log(Level.INFO, "Rotating one-time JWT signing key");
      boolean marked = jWTController.markOldSigningKeys();
      if (marked) {
        LOGGER.log(Level.INFO, "Marked old one-time JWT signing key, scheduling Sweeper");
        //(60000 + 60000)*2 = 240000 milliseconds = 4 min
        long duration = (Constants.DEFAULT_EXPIRY_LEEWAY * 1000 + Constants.ONE_TIME_JWT_LIFETIME_MS) * 2;
        TimerConfig config = new TimerConfig();
        // The Info of the Timer **MUST** start with 'Sweep'
        // Read comment of the method above
        config.setInfo("Sweep old one-time JWT signing key");
        config.setPersistent(false);
        timerService.createSingleActionTimer(duration, config);
      }
    } else if (timerInfo.startsWith("Sweep")) {
      LOGGER.log(Level.INFO, "Sweeping old one-time JWT signing key");
      try {
        jWTController.removeMarkedKeys();
        LOGGER.log(Level.INFO, "Deleted old one-time JWT signing key");
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to delete old one-time JWT signing key", e);
      }
    }
  }
}
