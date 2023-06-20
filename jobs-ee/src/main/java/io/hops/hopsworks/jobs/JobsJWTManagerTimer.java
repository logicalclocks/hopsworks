/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.jobs;

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

@Singleton
@Startup
public class JobsJWTManagerTimer {
  @EJB
  private JobsJWTManager jobsJWTManager;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 60000L; // 1 min
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Jobs JWT renew Manager",
      false));
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Timeout
  public void monitorJWTTimer() {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    
    jobsJWTManager.monitorJWT();
  }
}
