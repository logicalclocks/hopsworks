/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

 
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
  LOSTDEVICE("LOST DEVICE REPORT");

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
