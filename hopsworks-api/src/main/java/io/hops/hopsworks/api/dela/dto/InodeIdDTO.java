package io.hops.hopsworks.api.dela.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InodeIdDTO {
  private Integer id;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
}
