package io.hops.hopsworks.common.dao.user.security.audit;

public enum RolesAuditAction {

  // for adding role by the admin
  ROLE_ADDED("ADDED ROLE"),
  // for removing role by the admin
  ROLE_REMOVED("REMOVED ROLE"),
  ROLE_UPDATED("UPDATED ROLES"),
  SUCCESS("SUCCESS"),
  FAILED("FAILED"),
  // for getting all changing services
  ALL_SERVICE_STATUSES("ALL");

  private final String value;

  private RolesAuditAction(String value) {
    this.value = value;
  }

  @Override
  public String toString() { return value; }
}
