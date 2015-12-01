/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

 
public enum LoginsAuditActions {

  // for user authentication
  LOGIN("LOGIN"),
  // for user authentication
  LOGOUT("LOGOUT"),
  // to get registration audit logs
  REGISTRATION("REGISTRATION");

  private final String value;

  private LoginsAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static LoginsAuditActions getLoginsAuditActions(String text) {
    if (text != null) {
      for (LoginsAuditActions b : LoginsAuditActions.values()) {
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
