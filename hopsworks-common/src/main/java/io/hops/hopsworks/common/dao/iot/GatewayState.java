package io.hops.hopsworks.common.dao.iot;

public enum GatewayState {
  REGISTERED("Registered"),
  BLOCKED("Blocked");
  
  private final String name;
  
  @Override
  public String toString() {
    return name;
  }
  
  GatewayState(String name) {
    this.name = name;
  }
}
