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

package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.maintenance.Maintenance;
import io.hops.hopsworks.common.maintenance.MaintenanceController;
import io.hops.hopsworks.common.message.MessageController;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.io.Serializable;
import io.hops.hopsworks.common.util.Settings;

@ManagedBean(name = "maintenance")
@RequestScoped
public class MaintenanceBean implements Serializable {

  @EJB
  private MaintenanceController maintenanceController;

  @EJB
  private UserFacade userFacade;

  @EJB
  private MessageController messageController;

  public MaintenanceBean() {
  }

  public Maintenance getMaintenance() {
    return maintenanceController.getMaintenance();
  }

  public short getStatus() {
    return maintenanceController.getStatus();
  }

  public void setStatus(short status) {
    maintenanceController.setStatus(status);
  }

  public String getMessage() {
    return maintenanceController.getMessage();
  }

  public void setMessage(String message) {
    maintenanceController.setMessage(message);
  }

  public void update(short status, String message) {
    setStatus(status);
    setMessage(message);

    if (status == 1) {
      messageController.sendToMany(userFacade.findAllUsers(), userFacade.
              findByEmail(Settings.SITE_EMAIL),
              "Administration Message", message, "");
    }
  }

}
