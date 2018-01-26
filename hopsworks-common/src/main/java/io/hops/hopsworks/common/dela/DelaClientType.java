package io.hops.hopsworks.common.dela;

/**
 * used in delaClientCtrl.js - make sure to sync
 */
public enum DelaClientType {

  BASE_CLIENT("BASE_CLIENT"),
  FULL_CLIENT("FULL_CLIENT");

  public final String type;
  
  DelaClientType(String type) {
    this.type = type;
  }
  
  public static DelaClientType from(String clientType) {
    switch (clientType) {
      case "BASE_CLIENT":
        return BASE_CLIENT;
      case "FULL_CLIENT":
        return FULL_CLIENT;
      default:
        return BASE_CLIENT;
    }
  }
}
