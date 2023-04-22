/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.oauth2;

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
public class OauthLoginStateCleaner {
  private final static Logger LOGGER = Logger.getLogger(OauthLoginStateCleaner.class.getName());
  
  @EJB
  private OAuthController oAuthController;
  
  @Schedule(info = "Oauth Login State Cleaner")
  public void oauthLoginStateCleaner(Timer timer) {
    int count = oAuthController.cleanupLoginStates();
    LOGGER.log(Level.INFO, "{0} timer event: {1}, removed {2} login State.",
      new Object[]{timer.getInfo(), new Date(), count});
  }
}
