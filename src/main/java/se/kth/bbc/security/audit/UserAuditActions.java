package se.kth.bbc.security.audit;

public enum UserAuditActions {

  // for user authentication
  LOGIN("LOGIN"),
  // for user authentication
  LOGOUT("LOGOUT"),

  // get all the logs
  ALL("ALL"),
  
  SUCCESS("SUCCESS"),
  
  FAILED("FAILED"),
  
  ABORTED("ABORTED");

  private final String value;

  private UserAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static UserAuditActions getLoginsAuditActions(String text) {
    if (text != null) {
      for (UserAuditActions b : UserAuditActions.values()) {
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
