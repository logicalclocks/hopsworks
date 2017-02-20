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
