/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import com.google.common.annotations.VisibleForTesting;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class RemoteGroupToProjectMappingSync {
  private final static Logger LOGGER = Logger.getLogger(RemoteGroupToProjectMappingSync.class.getName());
  private final AtomicBoolean semaphore = new AtomicBoolean(true);

  @EJB
  private RemoteGroupMapping remoteGroupMapping;
  @EJB
  private Settings settings;
  @Resource
  TimerService timerService;

  private Timer timer;
  
  @PostConstruct
  public void init() {
    String intervalRaw = settings.ldapGroupMappingSyncInterval();
    if (intervalRaw.equals("0") || !settings.isLdapGroupMappingSyncEnabled()) {
      LOGGER.log(Level.INFO, "Remote Group To Project Member Mapping Timer NOT created.");
      return;
    }
    Pair<Long, String> interval = interval();

    TimerConfig timerConfig = new TimerConfig("Remote Group To Project Member Mapping", false);
    timer = timerService.createIntervalTimer(10000, interval.getLeft(), timerConfig);
    LOGGER.log(Level.INFO, "Remote Group To Project Member Mapping Timer created, interval " + interval.getRight());
  }

  protected Pair<Long, String> interval() {
    String intervalRaw = settings.ldapGroupMappingSyncInterval();
    if (!StringUtils.isNumeric(intervalRaw)) {
      Long intervalValue = settings.getConfTimeValue(intervalRaw);
      TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(intervalRaw);
      long interval = intervalTimeunit.toMillis(intervalValue);

      // Do not run more frequent than 2 minutes
      if (interval < TimeUnit.MINUTES.toMillis(2)) {
        intervalRaw = "2m";
        interval = TimeUnit.MINUTES.toMillis(2);
      }
      return Pair.of(interval, intervalRaw);
    } else {
      // For backwards compatibility if it is a number assume it is milliseconds
      return Pair.of(Long.getLong(intervalRaw), intervalRaw + "ms");
    }
  }
  
  @PreDestroy
  private void destroyTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Timeout
  public void performTimeout(Timer timer) {
    if (semaphore.compareAndSet(true, false)) {
      try {
        LOGGER.log(Level.FINE, "Running Remote Group To Project Member Mapping synchronization");
        remoteGroupMapping.syncMapping();
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Failed to synchronize remote groups with project members", ex);
      } finally {
        semaphore.set(true);
      }
    }
  }

  @VisibleForTesting
  protected void setSettings(Settings settings) {
    this.settings = settings;
  }
}
