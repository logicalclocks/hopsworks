package io.hops.hopsworks.common.dao.jobs.description;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AppIdDTO {

  String id;

  public AppIdDTO() {
  }

  public AppIdDTO(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
