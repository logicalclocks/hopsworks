/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
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
