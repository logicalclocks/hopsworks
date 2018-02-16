/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
