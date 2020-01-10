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
package io.hops.hopsworks.common.provenance.core;

import io.hops.hopsworks.common.util.Settings;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class ProvenanceCleaner {
  private final static Logger LOGGER = Logger.getLogger(ProvenanceCleaner.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ProvenanceCleanerController cleanerCtrl;
  @Resource
  TimerService timerService;
  
  private String lastIndexChecked = "";
  
  @PostConstruct
  private void init() {
    long cleanerPeriod = settings.getProvCleanerPeriod();
    LOGGER.log(Level.INFO, "timer - provenance cleaner - period:{0}s", cleanerPeriod);
    timerService.createTimer(cleanerPeriod * 1000, cleanerPeriod * 1000, "Timer for provenance cleaner.");
  }
  
  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }
  
  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  private void timeout(Timer timer) {
    int cleanupSize = settings.getProvCleanupSize();
    if(cleanupSize == 0) {
      return;
    }
    try {
      Pair<Integer, String> round = cleanerCtrl.indexCleanupRound(lastIndexChecked, cleanupSize);
      LOGGER.log(Level.FINE, "cleanup round - idx cleaned:{0} from:{1} to:{2}",
        new Object[]{round.getValue0(), lastIndexChecked, round.getValue1()});
      lastIndexChecked = round.getValue1();
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "cleanup round was not successful - error", e);
    }
  }
}
