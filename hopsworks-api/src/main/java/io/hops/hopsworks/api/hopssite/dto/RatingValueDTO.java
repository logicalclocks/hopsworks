package io.hops.hopsworks.api.hopssite.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RatingValueDTO {
  private int value;

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
