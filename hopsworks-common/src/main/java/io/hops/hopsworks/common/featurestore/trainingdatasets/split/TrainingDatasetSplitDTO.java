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

package io.hops.hopsworks.common.featurestore.trainingdatasets.split;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a training dataset split, can be converted to JSON or XML
 * representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"name", "percentage"})
public class TrainingDatasetSplitDTO {
  @XmlElement
  private String name;
  @XmlElement
  private Float percentage;
  
  public TrainingDatasetSplitDTO(){}
  
  public TrainingDatasetSplitDTO(String name, Float percentage) {
    this.name = name;
    this.percentage = percentage;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Float getPercentage() {
    return percentage;
  }
  
  public void setPercentage(Float percentage) {
    this.percentage = percentage;
  }
  
  @Override
  public String toString() {
    return "TrainingDatasetSplitDTO{" +
      "name='" + name + '\'' +
      ", percentage=" + percentage +
      '}';
  }
}
