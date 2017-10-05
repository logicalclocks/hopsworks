package io.hops.hopsworks.dela.dto.hopsworks;

import io.hops.hopsworks.dela.dto.hopssite.SearchServiceDTO;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

public class HopsworksSearchDTO {
  @XmlRootElement
  public static class Item implements Serializable {
    
    private String name;
    private String description;
    private String publicId;
    private String type;
    private boolean public_ds;
    private boolean localDataset;
    private boolean downloading;
    private float score;

    public Item() {
    }

    public Item(SearchServiceDTO.Item item) {
      this.type = "ds";
      this.public_ds = true;
      this.localDataset = false;
      this.downloading = false;
      
      this.publicId = item.getPublicDSId();
      this.name = item.getDataset().getName();
      this.description = item.getDataset().getDescription();
      this.score = item.getScore();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
    
    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public boolean getPublic_ds() {
      return public_ds;
    }

    public void setPublic_ds(boolean public_ds) {
      this.public_ds = public_ds;
    }

    public boolean isLocalDataset() {
      return localDataset;
    }

    public void setLocalDataset(boolean localDataset) {
      this.localDataset = localDataset;
    }

    public boolean isDownloading() {
      return downloading;
    }

    public void setDownloading(boolean downloading) {
      this.downloading = downloading;
    }

    public String getPublicId() {
      return publicId;
    }

    public void setPublicId(String publicId) {
      this.publicId = publicId;
    }

    public boolean isPublic_ds() {
      return public_ds;
    }
  }
}
