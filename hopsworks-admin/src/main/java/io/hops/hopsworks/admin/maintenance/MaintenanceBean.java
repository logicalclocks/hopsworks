/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
