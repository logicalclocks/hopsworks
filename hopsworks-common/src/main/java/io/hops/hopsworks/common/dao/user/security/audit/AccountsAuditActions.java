package io.hops.hopsworks.common.dao.user.security.audit;

public enum AccountsAuditActions {

  // for password change
  PASSWORDCHANGE("PASSWORD CHANGE"),
  // for security question change
  SECQUESTIONCHANGE("SECURITY QUESTION CHANGE"),
  // for profile update
  PROFILEUPDATE("PROFILE UPDATE"),
  // for approving/changing status of users
  USERMANAGEMENT("USER MANAGEMENT"),
  // for mobile lost or yubikeyu lost devices
  LOSTDEVICE("LOST DEVICE REPORT"),
  // for adminchange user account status
  CHANGEDSTATUS("CHANGED STATUS"),
  // to get registration audit logs
  REGISTRATION("REGISTRATION"),
  UNREGISTRATION("UNREGISTRATION"),
  RECOVERY("RECOVERY"),

  QRCODE("QR CODE"),

  SECQUESTION("SECURTY QUESTION RESET"),

  PROFILE("PROFILE UPDATE"),
  PASSWORD("PASSWORD CHANGE"),
  TWO_FACTOR("TWO FACTOR CHANGE"),
  // get all the logs
  ALL("ALL"),

  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  ABORTED("ABORTED");

  private final String value;

  private AccountsAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static AccountsAuditActions getAccountsAuditActions(String text) {
    if (text != null) {
      for (AccountsAuditActions b : AccountsAuditActions.values()) {
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
