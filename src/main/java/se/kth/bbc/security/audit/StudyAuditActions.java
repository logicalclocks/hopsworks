/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public enum StudyAuditActions {

  // for detialed audit info
  AUDITTRAILS("AUDIT TRAILS");

  private final String value;

  private StudyAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static StudyAuditActions getStudyAuditActions(String text) {
    if (text != null) {
      for (StudyAuditActions b : StudyAuditActions.values()) {
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
