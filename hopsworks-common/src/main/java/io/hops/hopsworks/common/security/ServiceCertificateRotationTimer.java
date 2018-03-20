/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class ServiceCertificateRotationTimer {
  private final static Logger LOG = Logger.getLogger(ServiceCertificateRotationTimer.class.getName());
  private final static String TO_BE_REVOKED = ".cert.pem.TO_BE_REVOKED.{COMMAND_ID}";
  
  @Resource
  private TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  @PostConstruct
  public void init() {
    String rawInterval = settings.getServiceKeyRotationInterval();
    Long intervalValue = Settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = Settings.getConfTimeTimeUnit(rawInterval);
    LOG.log(Level.INFO, "Service certificate rotation is configured to run every " + intervalValue + " " +
        intervalTimeunit.name());
    
    intervalValue = intervalTimeunit.toMillis(intervalValue);
    if (settings.isServiceKeyRotationEnabled()) {
      timerService.createTimer(intervalValue, intervalValue, "Service certificate rotation");
    }
  }
  
  @Timeout
  public void rotate(Timer timer) {
    LOG.log(Level.FINEST, "Rotating service certificates");
    certificatesMgmService.issueServiceKeyRotationCommand();
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String getToBeRevokedSuffix(String commandId) {
    return TO_BE_REVOKED.replaceAll("\\{COMMAND_ID\\}", commandId);
  }
}
