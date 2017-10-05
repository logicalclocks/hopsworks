package io.hops.hopsworks.dela.dto.hopssite;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RatingDTO {

  private int rate;
  private int ratedBy;

  public RatingDTO() {
  }

  public RatingDTO(int rate, int ratedBy) {
    this.rate = rate;
    this.ratedBy = ratedBy;
  }

  public int getRate() {
    return rate;
  }

  public void setRate(int rate) {
    this.rate = rate;
  }

  public int getRatedBy() {
    return ratedBy;
  }

  public void setRatedBy(int ratedBy) {
    this.ratedBy = ratedBy;
  }
}