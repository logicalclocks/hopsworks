/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class OauthLoginStateCleaner {
  private final static Logger LOGGER = Logger.getLogger(OauthLoginStateCleaner.class.getName());
  private final static long CLEANUP_INTERVAL = 1 * (24 * 60 * 60 * 1000);
  
  @EJB
  private OAuthController oAuthController;
  
  @Resource
  TimerService timerService;
  
  @PostConstruct
  private void init() {
    timerService.createTimer(0, CLEANUP_INTERVAL, "Oauth Login State Cleaner");
  }
  
  @PreDestroy
  private void destroyTimer() {
    for (Timer timer : timerService.getTimers()) {
      timer.cancel();
    }
  }
  
  @Timeout
  public void performTimeout(Timer timer) {
    int count = oAuthController.cleanupLoginStates();
    LOGGER.log(Level.INFO, "{0} timer event: {1}, removed {2} login State.",
      new Object[]{timer.getInfo(), new Date(), count});
  }
}
