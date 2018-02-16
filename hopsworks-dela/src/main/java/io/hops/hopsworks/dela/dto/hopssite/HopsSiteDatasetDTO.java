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
