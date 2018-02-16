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

package io.hops.hopsworks.common.maintenance;

import io.hops.hopsworks.common.util.Settings;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class MaintenanceController {

  private Maintenance maintenance;
  
  @EJB
  private Settings settings;

  public Maintenance getMaintenance() {
    if (maintenance == null) {
      maintenance = new Maintenance();
    }
    return maintenance;
  }

  public short getStatus() {
    return maintenance.getStatus();
  }

  public void setStatus(short status) {
    maintenance.setStatus(status);
  }

  public String getMessage() {
    return maintenance.getMessage();
  }

  public void setMessage(String message) {
    maintenance.setMessage(message);
  }
  
  public boolean isFirstTimeLogin() {
    return settings.getFirstTimeLogin().compareTo("1")==0;
  }
  
}
