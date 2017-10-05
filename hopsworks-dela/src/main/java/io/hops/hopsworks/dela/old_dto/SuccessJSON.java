package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SuccessJSON {

  private String details;

  public SuccessJSON() {
    this.details = "";
  }

  public SuccessJSON(String details) {
    this.details = details;
  }
  
  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }
}
