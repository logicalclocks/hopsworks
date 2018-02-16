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

package io.hops.hopsworks.dela.old_hopssite_dto;

import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;

@XmlRootElement
public class PopularDatasetJSON {

  private ManifestJSON manifestJson;

  private String datasetId;

  private int leeches;

  private int seeds;

  public PopularDatasetJSON(ManifestJSON manifestJson, String datasetId,
          int leeches, int seeds) {
    this.manifestJson = manifestJson;
    this.datasetId = datasetId;
    this.leeches = leeches;
    this.seeds = seeds;
  }

  public PopularDatasetJSON() {
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public void setLeeches(int leeches) {
    this.leeches = leeches;
  }

  public void setSeeds(int seeds) {
    this.seeds = seeds;
  }

  public ManifestJSON getManifestJson() {
    return manifestJson;
  }

  public void setManifestJson(ManifestJSON manifestJson) {
    this.manifestJson = manifestJson;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public int getLeeches() {
    return leeches;
  }

  public int getSeeds() {
    return seeds;
  }

}
