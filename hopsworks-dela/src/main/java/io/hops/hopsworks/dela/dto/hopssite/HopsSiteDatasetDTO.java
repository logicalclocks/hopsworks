package io.hops.hopsworks.dela.dto.hopssite;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HopsSiteDatasetDTO {

  private String publicId;
  private String name;
  private int version;
  private String description;
  private Date madePublicOn;
  private long dsSize;
  private Integer rating;
  private boolean localDataset;

  public HopsSiteDatasetDTO() {
  }

  public HopsSiteDatasetDTO(String publicId, String name, int version, String description, Date madePublicOn, 
    long dsSize, Integer rating) {
    this.publicId = publicId;
    this.name = name;
    this.version = version;
    this.description = description;
    this.madePublicOn = madePublicOn;
    this.dsSize = dsSize;
    this.rating = rating;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getMadePublicOn() {
    return madePublicOn;
  }

  public void setMadePublicOn(Date madePublicOn) {
    this.madePublicOn = madePublicOn;
  }

  public long getDsSize() {
    return dsSize;
  }

  public void setDsSize(long dsSize) {
    this.dsSize = dsSize;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public boolean isLocalDataset() {
    return localDataset;
  }

  public void setLocalDataset(boolean localDataset) {
    this.localDataset = localDataset;
  }

  @Override
  public String toString() {
    return "HopsSiteDatasetDTO{" + "publicId=" + publicId + ", name=" + name + ", dsSize=" + dsSize + ", rating="
            + rating + '}';
  }

}
