package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserContentsSummaryJSON {

  private Integer projectId;
  private ElementSummaryJSON[] elementSummaries = new ElementSummaryJSON[0];

  public UserContentsSummaryJSON() {
  }

  public UserContentsSummaryJSON(Integer projectId,
    ElementSummaryJSON[] elementSummaries) {
    this.projectId = projectId;
    if (elementSummaries != null) {
      this.elementSummaries = elementSummaries;
    }
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public ElementSummaryJSON[] getElementSummaries() {
    return elementSummaries;
  }

  public void setElementSummaries(ElementSummaryJSON[] elementSummaries) {
    if (elementSummaries != null) {
      this.elementSummaries = elementSummaries;
    } else {
      this.elementSummaries = new ElementSummaryJSON[0];
    }
  }

  @Override
  public String toString() {
    return "UserContentsSummaryJSON{" + "projectId=" + projectId + ", elementSummaries=" + elementSummaries + '}';
  }

}
