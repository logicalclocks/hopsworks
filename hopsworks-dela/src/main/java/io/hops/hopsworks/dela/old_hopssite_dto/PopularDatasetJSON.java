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
