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

import io.hops.hopsworks.common.util.PayaraClusterManager;

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
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class PermissionsCleaner {
  
  private final static Logger LOGGER = Logger.getLogger(PermissionsCleaner.class.getName());
  
  @EJB
  private PermissionsFixer permissionsFixer;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  private int counter = 0;
  
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 900000L; // 15 min
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Fix dataset permissions timer",
      false));
  }

  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  @Timeout
  public void fixDatasetPermissions(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    counter = permissionsFixer.fixPermissions(counter, System.currentTimeMillis());
    if (counter > 0) {
      LOGGER.log(Level.INFO, "Fix permissions triggered by timer counter={0}", counter);
    }
  }
}
