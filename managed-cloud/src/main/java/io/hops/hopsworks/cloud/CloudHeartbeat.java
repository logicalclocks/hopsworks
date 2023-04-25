/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class CloudHeartbeat {
  private static final Logger LOGGER = Logger.getLogger(CloudHeartbeat.class.getName());
  
  @EJB
  private CloudManager cloudManager;
  
  @Schedule(second = "*/3", minute = "*", hour = "*", info = "Cloud heartbeat")
  public void cloudHeartbeatTimer() {
    LOGGER.log(Level.FINE, "Hopsworks@Cloud - heartbeat");
    cloudManager.heartbeat();
  }
}
