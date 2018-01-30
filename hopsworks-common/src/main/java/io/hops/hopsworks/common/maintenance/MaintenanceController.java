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
