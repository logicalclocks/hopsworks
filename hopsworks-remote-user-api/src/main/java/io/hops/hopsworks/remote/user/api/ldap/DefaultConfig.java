/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.ldap;

public enum DefaultConfig {
  MIT_LDAP("MIT_LDAP", "uid", "uid=%s", ""),
  MIT_KRB("MIT_KRB", "uid", "uid=%s", "krbPrincipalName=%s"),
  ACTIVE_DIRECTORY("ACTIVE_DIRECTORY", "sAMAccountName=%s", "sAMAccountName=%s", "userPrincipalName=%s");
  
  private final String value;
  private final String userId;
  private final String searchFilter;
  private final String krbSearchFilter;
  
  DefaultConfig(String value, String userId, String searchFilter, String krbSearchFilter) {
    this.value = value;
    this.userId = userId;
    this.searchFilter = searchFilter;
    this.krbSearchFilter = krbSearchFilter;
  }
  
  public static DefaultConfig fromString(String value) {
    return valueOf(value.toUpperCase());
  }
  
  public String getValue() {
    return value;
  }
  
  public String getUserId() {
    return userId;
  }
  
  public String getSearchFilter() {
    return searchFilter;
  }
  
  public String getKrbSearchFilter() {
    return krbSearchFilter;
  }
  
  @Override
  public String toString() {
    return value;
  }
}
