/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.jobs;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class JobsJWTManagerTimer {
  @EJB
  private JobsJWTManager jobsJWTManager;
  
  @Schedule(minute = "*/1", hour = "*", info = "Jobs JWT renew Manager")
  public void monitorJWTTimer() {
    jobsJWTManager.monitorJWT();
  }
}
