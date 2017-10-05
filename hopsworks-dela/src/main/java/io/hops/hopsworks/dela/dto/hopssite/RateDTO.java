package io.hops.hopsworks.dela.dto.hopssite;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RateDTO {

  private int rating;
  private String userEmail;

  public RateDTO() {
  }

  public RateDTO(String userEmail, int rating) {
    this.rating = rating;
    this.userEmail = userEmail;
  }
  
  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }
}