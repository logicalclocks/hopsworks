package io.hops.hopsworks.dela.old_dto;

public class HopsResource {

  private final int projectId;

  public HopsResource(int projectId) {
    this.projectId = projectId;
  }

  public int getProjectId() {
    return projectId;
  }
}
