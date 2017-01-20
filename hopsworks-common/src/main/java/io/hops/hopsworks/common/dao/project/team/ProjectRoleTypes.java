package io.hops.hopsworks.common.dao.project.team;

public enum ProjectRoleTypes {

  DATA_OWNER("Data owner"),
  DATA_SCIENTIST("Data scientist");

  String role;

  ProjectRoleTypes(String role) {
    this.role = role;
  }

  public String getTeam() {
    return this.role;
  }

}
