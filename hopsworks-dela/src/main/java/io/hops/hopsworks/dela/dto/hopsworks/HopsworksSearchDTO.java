/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.dela.dto.hopsworks;

import io.hops.hopsworks.dela.dto.hopssite.SearchServiceDTO;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

public class HopsworksSearchDTO {
  @XmlRootElement
  public static class Item implements Serializable {
    
    private String name;
    private int version;
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
      this.version = item.getDataset().getVersion();
      this.description = item.getDataset().getDescription();
      this.score = item.getScore();
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
