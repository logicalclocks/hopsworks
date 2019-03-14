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

package io.hops.hopsworks.dela;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DelaException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
public class DelaStateController {

  private static final Logger LOGGER = Logger.getLogger(DelaStateController.class.getName());

  @EJB
  private Settings settings;

  private boolean delaEnabled = false;
  private boolean delaCertsAvailable = false;
  private boolean transferDelaAvailable = false;
  private boolean hopssiteAvailable = false;
  
  private KeyStore keystore;
  private KeyStore truststore;
  private String keystorePassword;
      
  @PostConstruct
  private void init() {
    if (settings.isDelaEnabled()) {
      delaEnabled = settings.isDelaEnabled();
      LOGGER.log(Level.INFO, "dela enabled");
    } else {
      LOGGER.log(Level.INFO, "dela disabled");
    }
  }

  public boolean delaEnabled() {
    return delaEnabled;
  }
  
  public boolean delaAvailable() {
    return hopsworksDelaSetup() && transferDelaAvailable && hopssiteAvailable;
  }

  public void checkDelaAvailable() throws DelaException {
    if (!delaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_NOT_AVAILABLE, Level.SEVERE, DelaException.Source.LOCAL);
    }
  }

  public boolean transferDelaAvailable() {
    return hopsworksDelaSetup() && transferDelaAvailable;
  }

  public boolean hopssiteAvailable() {
    return hopsworksDelaSetup() && hopssiteAvailable;
  }
  
  public void checkHopssiteAvailable() throws DelaException {
    if (!hopssiteAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.HOPSSITE_NOT_AVAILABLE, Level.SEVERE, DelaException.Source.LOCAL);
    }
  }

  public boolean hopsworksDelaSetup() {
    return delaEnabled && delaCertsAvailable;
  }

  public void checkHopsworksDelaSetup() throws DelaException {
    if (!hopsworksDelaSetup()) {
      throw new DelaException(RESTCodes.DelaErrorCode.REMOTE_DELA_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
  }

  public void transferDelaContacted() {
    transferDelaAvailable = true;
  }

  public void hopssiteContacted() {
    hopssiteAvailable = true;
  }
  
  public void hopssiteCertsAvailable(KeyStore keystore, KeyStore truststore, String keystorePassword) {
    delaCertsAvailable = true;
    this.keystore = keystore;
    this.truststore = truststore;
    this.keystorePassword = keystorePassword;
  }

  public KeyStore getKeystore() {
    return keystore;
  }

  public KeyStore getTruststore() {
    return truststore;
  }

  public String getKeystorePassword() {
    return keystorePassword;
  }
}
