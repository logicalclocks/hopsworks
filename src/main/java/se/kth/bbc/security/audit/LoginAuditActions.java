package se.kth.bbc.security.audit;

public enum LoginAuditActions {

  // for user authentication
  LOGIN("LOGIN"),
  // for user authentication
  LOGOUT("LOGOUT"),
  // to get registration audit logs
  REGISTRATION("REGISTRATION"),
  // get all the logs
  ALL("ALL");

  private final String value;

  private LoginAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static LoginAuditActions getLoginsAuditActions(String text) {
    if (text != null) {
      for (LoginAuditActions b : LoginAuditActions.values()) {
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
