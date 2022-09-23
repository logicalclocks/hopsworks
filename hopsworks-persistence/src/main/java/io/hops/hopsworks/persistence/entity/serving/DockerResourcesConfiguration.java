/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerResourcesConfiguration {

  public DockerResourcesConfiguration(){}

  public DockerResourcesConfiguration(double cores, int memory, int gpus) {
    this.cores = cores;
    this.memory = memory;
    this.gpus = gpus;
  }
  
  private double cores = 1;

  private int memory = 1024;

  private int gpus = 0;

  public double getCores() {
    return cores;
  }

  public void setCores(double cores) {
    this.cores = cores;
  }

  public int getMemory() {
    return memory;
  }

  public void setMemory(int memory) {
    this.memory = memory;
  }

  public int getGpus() {
    return gpus;
  }

  public void setGpus(int gpus) {
    this.gpus = gpus;
  }

  @Override
  public final boolean equals(Object object) {
    if (!(object instanceof DockerResourcesConfiguration)) {
      return false;
    }
    DockerResourcesConfiguration other = (DockerResourcesConfiguration) object;
    if (this.cores != other.cores || this.memory != other.memory || this.gpus != other.gpus) {
      return false;
    }
    return true;
  }
}
