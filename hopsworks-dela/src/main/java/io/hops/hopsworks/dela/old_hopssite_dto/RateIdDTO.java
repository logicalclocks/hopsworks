package io.hops.hopsworks.dela.old_hopssite_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RateIdDTO {
  private Integer id;

  public RateIdDTO() {
  }

  public RateIdDTO(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
}
