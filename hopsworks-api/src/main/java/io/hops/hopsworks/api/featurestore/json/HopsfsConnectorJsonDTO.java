/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.json;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO representing the JSON payload from REST-clients when creating/updating Hopsfs storage backends in the
 * featurestore
 */
@XmlRootElement
public class HopsfsConnectorJsonDTO extends StorageConnectorJsonDTO {
  
  private String hopsFsDataset;

  public HopsfsConnectorJsonDTO() {
  }
  
  public HopsfsConnectorJsonDTO(String hopsFsDataset) {
    this.hopsFsDataset = hopsFsDataset;
  }
  
  public HopsfsConnectorJsonDTO(String name, String description, String hopsFsDataset) {
    super(name, description);
    this.hopsFsDataset = hopsFsDataset;
  }
  
  @XmlElement
  public String getHopsFsDataset() {
    return hopsFsDataset;
  }
  
  public void setHopsFsDataset(String hopsFsDataset) {
    this.hopsFsDataset = hopsFsDataset;
  }
}
