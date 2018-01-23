package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServingEndpointJsonDTO extends KeystoreDTO {

  private String model;
  private String project;

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }
}
