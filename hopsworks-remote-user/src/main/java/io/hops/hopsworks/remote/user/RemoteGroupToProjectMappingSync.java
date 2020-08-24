/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.util.Settings;

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
public class RemoteGroupToProjectMappingSync {
  private final static Logger LOGGER = Logger.getLogger(RemoteGroupToProjectMappingSync.class.getName());

  @EJB
  private RemoteGroupMapping remoteGroupMapping;
  @EJB
  private Settings settings;
  @Resource
  TimerService timerService;
  
  @PostConstruct
  public void init() {
    long interval = settings.ldapGroupMappingSyncInterval();
    if (interval < 1) {//minimum 1hour
      LOGGER.log(Level.INFO, "Remote Group To Project Member Mapping Timer NOT created.");
      return;
    }
    long intervalDuration = interval * 60 * 60 * 1000;
    TimerConfig timerConfig = new TimerConfig("Remote Group To Project Member Mapping", false);
    timerService.createIntervalTimer(intervalDuration, intervalDuration, timerConfig);
    LOGGER.log(Level.INFO, "Remote Group To Project Member Mapping Timer created.");
  }
  
  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }
  
  @Timeout
  public void performTimeout(Timer timer) {
    remoteGroupMapping.syncMapping();
    LOGGER.log(Level.INFO, "Remote Group To Project Member Mapping Timer expired.");
  }
  
}
