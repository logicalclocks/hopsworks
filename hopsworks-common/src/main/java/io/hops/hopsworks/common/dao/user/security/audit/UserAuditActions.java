package io.hops.hopsworks.common.dao.user.security.audit;

public enum UserAuditActions {

  LOGIN("LOGIN"),
  LOGOUT("LOGOUT"),
  UNAUTHORIZED("UNAUTHORIZED ACCESS"),
  SUCCESS("SUCCESS"),
  FAILED("FAILED"),
  ABORTED("ABORTED"),
  ALL("ALL");

  private final String value;

  UserAuditActions(String value) { this.value = value; }

  @Override
  public String toString() { return value; }
}
