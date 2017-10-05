package io.hops.hopsworks.api.hopssite.dto;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RateReqDTO {
  
  private int rating;
  private Date datePublished;
  private DatasetReqDTO dataset;

  public RateReqDTO() {
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public Date getDatePublished() {
    return datePublished;
  }

  public void setDatePublished(Date datePublished) {
    this.datePublished = datePublished;
  }

  public DatasetReqDTO getDataset() {
    return dataset;
  }

  public void setDataset(DatasetReqDTO dataset) {
    this.dataset = dataset;
  }

}