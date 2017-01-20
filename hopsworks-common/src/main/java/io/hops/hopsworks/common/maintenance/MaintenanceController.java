package io.hops.hopsworks.common.maintenance;

import javax.ejb.Stateless;

@Stateless
public class MaintenanceController {

  private Maintenance maintenance;

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
}
