package se.kth.bbc.study;

/**
 *
 * @author roshan
 */
public enum StudyRoleTypes {

  MASTER("Master"),
  RESEARCHER("Researcher"),
  AUDITOR("Auditor");

  String role;

  StudyRoleTypes(String role) {
    this.role = role;
  }

  public String getTeam() {
    return this.role;
  }

}
