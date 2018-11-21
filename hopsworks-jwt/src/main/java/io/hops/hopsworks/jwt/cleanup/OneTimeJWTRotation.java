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
package io.hops.hopsworks.jwt.cleanup;

import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
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

@Startup
@Singleton
public class OneTimeJWTRotation {

  private final static Logger LOGGER = Logger.getLogger(OneTimeJWTRotation.class.getName());
  private final static long TIMER_INTERVAL = 1 * (24 * 60 * 60 * 1000);

  @EJB
  private JWTController jWTController;
  @Resource
  TimerService timerService;

  @PostConstruct
  private void init() {
    timerService.createTimer(0, TIMER_INTERVAL, "Mark");
  }

  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }

  private void markAndSetTimer() {
    boolean marked = jWTController.markOldSigningKeys();
    if (marked) {
      //(60000 + 60000)*2 = 240000 milliseconds = 4 min
      long duration = (Constants.DEFAULT_EXPIRY_LEEWAY * 1000 + Constants.ONE_TIME_JWT_LIFETIME_MS) * 2;
      TimerConfig config = new TimerConfig();
      config.setInfo("Remove");
      timerService.createSingleActionTimer(duration, config);
    }
  }

  @Timeout
  public void performTimeout(Timer timer) {
    String name = (String) timer.getInfo();
    if ("Mark".equals(name)) {
      markAndSetTimer();
    } else {
      jWTController.removeMarkedKeys();
    }
    LOGGER.log(Level.INFO, "{0} timer event: {1}.", new Object[]{timer.getInfo(), new Date()});
  }
}
