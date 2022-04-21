/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.opensearch;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OpenSearchHitDTO {
  private OpenSearchProjectDTO projects;
  private OpenSearchDatasetDTO datasets;
  private OpenSearchInodeDTO inodes;
  
  public OpenSearchHitDTO(){
  }
  
  public OpenSearchProjectDTO getProjects() {
    return projects;
  }
  
  public void setProjects(OpenSearchProjectDTO projects) {
    this.projects = projects;
  }
  
  public OpenSearchDatasetDTO getDatasets() {
    return datasets;
  }
  
  public void setDatasets(OpenSearchDatasetDTO datasets) {
    this.datasets = datasets;
  }
  
  public OpenSearchInodeDTO getInodes() {
    return inodes;
  }
  
  public void setInodes(OpenSearchInodeDTO inodes) {
    this.inodes = inodes;
  }
  
  @Override
  public String toString() {
    return "OpenSearchHitDTO{" +
      "projects=" + projects +
      ", datasets=" + datasets +
      ", inodes=" + inodes +
      '}';
  }
}
