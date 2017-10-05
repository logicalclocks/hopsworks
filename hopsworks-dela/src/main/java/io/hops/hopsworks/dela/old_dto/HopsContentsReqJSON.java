package io.hops.hopsworks.dela.old_dto;

import java.util.List;

public class HopsContentsReqJSON {

  private List<Integer> projectIds;

  public HopsContentsReqJSON() {
  }

  public HopsContentsReqJSON(List<Integer> projectIds) {
    this.projectIds = projectIds;
  }

  public List<Integer> getProjectIds() {
    return projectIds;
  }

  public void setProjectIds(List<Integer> projectId) {
    this.projectIds = projectId;
  }
}
