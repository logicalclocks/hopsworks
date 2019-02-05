/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
