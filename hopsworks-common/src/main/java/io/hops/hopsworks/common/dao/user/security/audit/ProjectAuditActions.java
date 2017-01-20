package io.hops.hopsworks.common.dao.user.security.audit;

public enum ProjectAuditActions {

  // for detialed audit info
  AUDITTRAILS("AUDIT TRAILS");

  private final String value;

  private ProjectAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ProjectAuditActions getProjectAuditActions(String text) {
    if (text != null) {
      for (ProjectAuditActions b : ProjectAuditActions.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
