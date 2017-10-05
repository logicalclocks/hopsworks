package io.hops.hopsworks.dela.hopssite;

public enum HopsSiteExceptions {
  ALREADY_REGISTERED;
  
  public boolean is(String msg) {
    if(this.name().toLowerCase().equals(msg.toLowerCase())) {
      return true;
    }
    return false;
  }
}
