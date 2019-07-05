package io.hops.hopsworks.common.dao.iot;

public enum IotGatewayState {
  ACTIVE("Active"),
  BLOCKED("Blocked"),
  INACTIVE_BLOCKED("InactiveBlocked");
  
  private final String name;
  
  @Override
  public String toString() {
    return name;
  }
  
  IotGatewayState(String name) {
    this.name = name;
  }
}
